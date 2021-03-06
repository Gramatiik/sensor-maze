package fr.gramatiik.sensormaze.engines;

import fr.gramatiik.sensormaze.MainActivity;
import fr.gramatiik.sensormaze.models.Bloc;
import fr.gramatiik.sensormaze.models.Boule;
import android.app.Service;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe MazeEngine.
 *
 * Cette classe gère la partie physique du jeu SensorMaze.
 * Elle prend également en charge les différents capteur utilisés et
 * met à jour les valeurs qui en dépendent.
 *
 * Le moteur se charge également de créer les blocs et la boule via une fonction statique.
 *
 * @author Stanyslas Bres
 */
public class MazeEngine {
    private Boule mBoule = null;

    private List<Bloc> mBlocks = null;

    private MainActivity mActivity = null;
    private MazeView mViewEngine = null;

    private SensorManager mManager = null;
    private Sensor mAccelerometre = null;
    private Sensor mMagneticSensor = null;
    private Sensor mLightSensor = null;

    /**
     * Listener attaché à l'accéléromètre, gère le déplacement de la boule.
     */
    private SensorEventListener mAccelerometerSensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent pEvent) {
            float x = pEvent.values[0];
            float y = pEvent.values[1];

            if(mBoule != null) {
                // On met à jour les coordonnées de la boule
                RectF hitBox = mBoule.putXAndY(x, y);

                // Pour tous les blocs du labyrinthe
                for(Bloc block : mBlocks) {
                    // On crée un nouveau rectangle pour ne pas modifier celui du bloc
                    RectF inter = new RectF(block.getRectangle());
                    if(inter.intersect(hitBox)) {
                        // On agit différement en fonction du type de bloc
                        switch(block.getType()) {
                            case TROU:
                                mActivity.showDefeatDialog();
                                break;

                            case DEPART:
                                break;

                            case ARRIVEE:
                                mActivity.showVictoryDialog();
                                break;
                        }
                        break;
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor pSensor, int pAccuracy) {}
    };

    /**
     * Listener attaché au capteur magnétique, gère le changement de couleur de la boule.
     */
    private SensorEventListener mMagneticSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(mBoule != null) {
                // on calcule la couleur en fonction du magnétisme
                float maxRange = 50;

                int r = (int)sensorRangeToColorComponent(event.values[0], maxRange);
                int g = (int)sensorRangeToColorComponent(event.values[1], maxRange);
                int b = (int)sensorRangeToColorComponent(event.values[2], maxRange);

                int c = Color.rgb(r, g, b);
                // on applique la couleur à la boule
                mBoule.setCouleur(c);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    /**
     * Listener attaché au capteur de luminosité, gère le changement de couleur du fond.
     */
    private SensorEventListener mLightSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            int colorComponent = 255 - (int)event.values[2];
            mViewEngine.setBGColor(Color.rgb(colorComponent, colorComponent, colorComponent));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    /**
     * Récupère la Boule utilisée par le moteur physique
     * @return Boule référence vers la boule utilisée par le moteur
     */
    public Boule getBoule() {
        return mBoule;
    }

    /**
     * Change la Boule utilisée par le moteur physique
     * @param pBoule Nouvelle Boule
     */
    public void setBoule(Boule pBoule) {
        this.mBoule = pBoule;
    }

    /**
     * Calcule une couleur en fonction de la valeur du capteur et de sa valeur maximale
     *
     * @param value Valeur du capteur
     * @param maxValue Valeur maximale du capteur
     * @return float Valeur de couleur [0..255] de la composante
     */
    private float sensorRangeToColorComponent(float value, float maxValue) {
        if(value > maxValue) value = maxValue;
        return (Math.abs(value)*255)/maxValue;
    }

    /**
     * Constructeur du moteur physique,
     * initialise les capteurs.
     *
     * @param pView Référence vers l'activité appelante
     * @param mazeView Référence vers le moteur graphique
     */
    public MazeEngine(MainActivity pView, MazeView mazeView) {
        mActivity = pView;
        mViewEngine = mazeView;
        mManager = (SensorManager) mActivity.getBaseContext().getSystemService(Service.SENSOR_SERVICE);
        mAccelerometre = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticSensor = mManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mLightSensor = mManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    /**
     * Remet à zéro l'emplacement de la boule
     */
    public void reset() {
        mBoule.reset();
    }

    /**
     * Arrête les capteurs
     */
    public void stop() {
        mManager.unregisterListener(mAccelerometerSensorEventListener, mAccelerometre);
        mManager.unregisterListener(mMagneticSensorEventListener, mMagneticSensor);
        mManager.unregisterListener(mLightSensorEventListener, mLightSensor);
    }

    /**
     * Redémarre les capteurs
     */
    public void resume() {
        mManager.registerListener(mAccelerometerSensorEventListener, mAccelerometre, SensorManager.SENSOR_DELAY_GAME);
        mManager.registerListener(mMagneticSensorEventListener, mMagneticSensor, SensorManager.SENSOR_DELAY_GAME);
        mManager.registerListener(mLightSensorEventListener, mLightSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    /**
     * Construit le labyrinthe
     *
     * @param screenSizes Taille de l'écran
     * @return List<Bloc> Blocs générés, correspond à la 'carte' du labyrinthe
     */
    public List<Bloc> buildLabyrinthe(Point screenSizes) {
        // calculate screen sizes
        float tileSizeX = screenSizes.x / (float)20;
        float tileSizeY = screenSizes.y / (float)14;
        mBlocks = new ArrayList<>();
        mBlocks.add(new Bloc(Bloc.Type.TROU, 0, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 0, 1, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 0, 2, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 0, 3, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 0, 4, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 0, 5, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 0, 6, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 0, 7, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 0, 8, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 0, 9, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 0, 10, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 0, 11, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 0, 12, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 0, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 1, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 1, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 2, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 2, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 3, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 3, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 4, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 4, 1, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 4, 2, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 4, 3, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 4, 4, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 4, 5, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 4, 6, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 4, 7, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 4, 8, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 4, 9, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 4, 10, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 4, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 5, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 5, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 6, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 6, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 7, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 7, 1, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 7, 2, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 7, 5, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 7, 6, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 7, 9, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 7, 10, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 7, 11, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 7, 12, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 7, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 8, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 8, 5, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 8, 9, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 8, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 9, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 9, 5, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 9, 9, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 9, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 10, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 10, 5, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 10, 9, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 10, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 11, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 11, 5, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 11, 9, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 11, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 12, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 12, 1, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 12, 2, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 12, 3, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 12, 4, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 12, 5, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 12, 9, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 12, 8, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 12, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 13, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 13, 8, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 13, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 14, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 14, 8, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 14, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 15, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 15, 8, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 15, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 16, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 16, 4, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 16, 5, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 16, 6, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 16, 7, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 16, 8, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 16, 9, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 16, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 17, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 17, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 18, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 18, 13, tileSizeX, tileSizeY));

        mBlocks.add(new Bloc(Bloc.Type.TROU, 19, 0, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 19, 1, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 19, 2, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 19, 3, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 19, 4, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 19, 5, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 19, 6, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 19, 7, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 19, 8, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 19, 9, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 19, 10, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 19, 11, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 19, 12, tileSizeX, tileSizeY));
        mBlocks.add(new Bloc(Bloc.Type.TROU, 19, 13, tileSizeX, tileSizeY));

        Bloc b = new Bloc(Bloc.Type.DEPART, 2, 2, tileSizeX, tileSizeY);
        mBoule.setInitialRectangle(new RectF(b.getRectangle()));
        mBlocks.add(b);

        mBlocks.add(new Bloc(Bloc.Type.ARRIVEE, 8, 11, tileSizeX, tileSizeY));

        return mBlocks;
    }

}
