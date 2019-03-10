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
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class StetjeBesed {

    private static class RazdeliBesede
            extends Mapper<Object, Text, Text, IntWritable> {

        private final static IntWritable one = new IntWritable(1);
        private final static IntWritable zero = new IntWritable(0);
        private Text foundWord = new Text();
        private List<String> wordList;

        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            String[] words = value.toString().split("\\s");
            // Pridobi seznam iskanih besed in ga pretvori v list
            Configuration conf = context.getConfiguration();
            wordList = Arrays.asList(conf.getStrings("SeznamIskanihBesed"));
            ArrayList<String> remainingWords = new ArrayList<>(wordList);


            // Sprehodi se po besedah v dani datoteki in preveri ali je v seznamu
            for (String word : words) {
                if (wordList.contains(word)) {
                    foundWord.set(word);
                    context.write(foundWord, one);
                    // Odstrani besedo iz preostalih besed
                    // (v kolikor se katera ne pojavi izpisemo na koncu 0)
                    remainingWords.remove(word);
                }
            }
            // Izpisi se 0 za besede, ki se niso pojavile v povedi
            for (String word : remainingWords) {
                foundWord.set(word);
                context.write(foundWord, zero);
            }

        }
    }

    private static class PrestejBesede extends
            Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();

        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws InterruptedException, IOException {
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

        // Nastavi podane besede za parameter Mapperja
        conf.setStrings("SeznamIskanihBesed",
                        Arrays.copyOfRange(args, 2, args.length));

        // configuration should contain reference to your namenode
        FileSystem fs = FileSystem.get(new Configuration());
        // true stands for recursively deleting the folder you gave
        // (pobrise output direktorij, drugace MapReduce jamra, da ze obstaja)
        fs.delete(new Path(args[1]), true);

        Job job = Job.getInstance(conf, "Stetje besed v povedih");

        // Nastavi nase Mapper in Reducer razrede in formate izhoda
        job.setJarByClass(StetjeBesed.class);
        job.setMapperClass(RazdeliBesede.class);
        job.setReducerClass(PrestejBesede.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        // Dodaj vhodne in izhodne poti
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // Zazeni MR opravilo
        // ToolRunner boljse?
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
