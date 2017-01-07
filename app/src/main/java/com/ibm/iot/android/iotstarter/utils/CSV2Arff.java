package com.ibm.iot.android.iotstarter.utils;

/**
 * Created by Dennis on 2016-12-05 v: 49.
 */
        import weka.core.Instances;
        import weka.core.converters.ArffSaver;
        import weka.core.converters.CSVLoader;

        import java.io.File;
        import java.io.IOException;

public class CSV2Arff {
    /**
     * takes 2 arguments:
     * - CSV input file
     * - ARFF output file
     */
    public void convert(){

        // load CSV
        CSVLoader loader = new CSVLoader();
        try {
            loader.setSource(new File("src/resources/traindatatest.csv"));

            Instances data = loader.getDataSet();

            // save ARFF
            ArffSaver saver = new ArffSaver();
            saver.setInstances(data);
            saver.setFile(new File("src/resources/traindatatest.arff"));
            saver.writeBatch();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
