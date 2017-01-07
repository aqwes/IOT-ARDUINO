package com.ibm.iot.android.iotstarter.utils;

import android.content.Context;
import android.content.res.Resources;

import com.ibm.iot.android.iotstarter.R;

//import weka.classifiers.Classifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.filters.unsupervised.attribute.Remove;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static android.R.id.text1;


public class WekaUtils {
    Instance single_window;
    private Context applicationContext;
    Classifier classifier;




    public WekaUtils(Context applicationContext) {
        this.applicationContext=applicationContext;
    }

    public void runProgram() throws Exception {
        Resources res = applicationContext.getResources();
        System.out.println(res.openRawResource(R.raw.test).toString());

        BufferedReader reader = new BufferedReader(new InputStreamReader(res.openRawResource(R.raw.test)));
        Instances testdata = new Instances(reader);
        reader.close();
        testdata.setClassIndex(testdata.numAttributes() - 1);

        BufferedReader reader2 = new BufferedReader(
                new InputStreamReader(res.openRawResource(R.raw.traindatatest)));
        Instances traindata = new Instances(reader2);


        reader.close();
        traindata.setClassIndex(traindata.numAttributes() - 1);


        Instances train = traindata;
        Instances test = testdata;
        // train classifier
        Remove rm = new Remove();
        classifier = new J48();
        FilteredClassifier fc = new FilteredClassifier();
        fc.setFilter(rm);
        fc.setClassifier(classifier);
        Evaluation eval = null;
        try {
            fc.buildClassifier(train);
            for (int i = 0; i < test.numInstances(); i++) {

                double pred = fc.classifyInstance(test.instance(i));
               // double pred2 = fc.classifyInstance(single_window);
                System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX predicted: " + test.classAttribute().value((int) pred)+"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
                //System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX predicted: " + test.classAttribute().value((int) pred2)+"XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");






            }



            // evaluate classifier and print some statistics
            eval = new Evaluation(train);
            eval.evaluateModel(classifier, test);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(eval.toSummaryString("\nResults\n======\n", false));




    }

}
