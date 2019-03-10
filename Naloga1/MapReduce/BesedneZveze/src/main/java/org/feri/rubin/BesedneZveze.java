package org.feri.rubin;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author David Rubin
 * 2019, FERI, AAMP
 */
public class BesedneZveze {

    /**
     * Mapper razred za stetje poljubno dolgih besedniz zvez (parameter pri zagonu programa)
     *
     * Razdeli podano poved na skupine besednih zvez (v kolikor stevilo besed ni delijivo s
     * podanim parametrom vzame krajso besedno zvezo).
     */
    public static class RazdeliBesedneZveze
            extends Mapper<Object, Text, Text, IntWritable> {

        // Hadoop zeli svoj razred pri izhodu map funkcije
        private final static IntWritable one = new IntWritable(1);


        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            // Pridobi konfiguracijo (stevilo besed v besedni zvezi), default: 1
            Configuration conf = context.getConfiguration();
            int steviloBesed = conf.getInt("DolzinaBesedneZveze", 1);
            // Spremeni pridobljeno poved v seznam besed
            String[] sentenceWords = value.toString().split("\\s+");

            for (int i=0; i<sentenceWords.length; i++) {

                int wordLimit = i + steviloBesed;
                // V kolikor s besedno zvezo presezemo dolzino povedi, jo preskoci
                if (wordLimit > sentenceWords.length) break;

                String besednaZveza = String.join(" ",
                        Arrays.copyOfRange(sentenceWords, i, wordLimit));
                context.write(new Text(besednaZveza), one);
            }
        }

    }

    /**
     * Reducer Class presteje pojavitev za podane besedne zveze (kljuci) in jih zapise v rezultat.
     */
    public static class PrestejBesedneZveze extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();

        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable v : values) {
                sum += v.get();
            }
            result.set(sum);
            context.write(key, result);

        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        conf.set("mapred.job.queue.name", "default");
        // Nastavi parameter za dolzino besedne zveze
        conf.setInt("DolzinaBesedneZveze", Integer.parseInt(args[2]));

        // configuration should contain reference to your namenode
        FileSystem fs = FileSystem.get(new Configuration());
        // true stands for recursively deleting the folder you gave
        // (pobrise output direktorij, drugace MapReduce jamra, da ze obstaja)
        fs.delete(new Path(args[1]), true);

        Job job = Job.getInstance(conf, "Stetje besednih zvez v povedi");

        job.setJarByClass(BesedneZveze.class);
        job.setMapperClass(RazdeliBesedneZveze.class);
        job.setReducerClass(PrestejBesedneZveze.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
