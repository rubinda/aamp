Pri tej nalogi se bomo spoznali orodjem za distributirano shrambo podatkov Hadoop ter s programskim modelom MapReduce, ki je namenjen obdelavi velikih podatkovnih zbirk paralelno in distributirano na gruči strežnikov.

Najprej moramo vzpostaviti skupino strežnikov, ki bodo imeli nameščeni sistem Apache Hadoop ter bodo tvorili gručo z enim glavnim (master) strežnikom ter tremi podpornimi (slave) strežniki. V ta namen bomo uporabili programsko orodje Docker za pripravo virtualizacije omenjene gruče strežnikov. 

Samo programsko orodje najdete na: [Docker](https://download.docker.com/win/stable/Docker%20for%20Windows%20Installer.exe)

Kot osnovno sliko, ki jo bomo uporabili pri virtualizaciji, bomo imeli osnovano na operacijskem sistemu linux:18.04. Samo programsko orodje Docker pa nam bo pri vzpostavitvi virtualizacije posameznega strežnika namestil še željene nadgraditve, ki so v našem primeru Apache Hadoop, YARN, MapReduce ter še podporo HDFS datotečnem sistemu. Vsi podatki glede namestitve so podani v docker datoteki.

[Docker Image](https://github.com/kiwenlau/hadoop-cluster-docker)

###### Based on https://github.com/kiwenlau/hadoop-cluster-docker/blob/master/README.md

##### 1. Build docker
Za izvedbo ukaza morate biti v direktoriju "..\hadoop-cluster-docker\"
```cmd
docker build -t gemma/hadoop:1.0 .
```

##### 3. create hadoop network
```cmd
docker network create --driver=bridge hadoopNetwork
```

##### 4. Run master
```cmd
docker run -itd --net=hadoopNetwork -p 50070:50070 -p 8088:8088 --name hadoop-master --hostname hadoop-master gemma/hadoop:1.0
```

##### 5. Run slaves
```cmd
docker run -itd --net=hadoopNetwork --name hadoop-slave1 --hostname hadoop-slave1 gemma/hadoop:1.0
docker run -itd --net=hadoopNetwork --name hadoop-slave2 --hostname hadoop-slave2 gemma/hadoop:1.0
docker run -itd --net=hadoopNetwork --name hadoop-slave3 --hostname hadoop-slave3 gemma/hadoop:1.0
```

##### 6. Preveri zagnane instance
```cmd
docker ps
```

##### 7. Zaženi glavni kontejner
```cmd
docker exec -it hadoop-master bash
```

##### 7.1 Zaženi/ustavi hadoop
```cmd
sh start-hadoop.sh
sh stop-hadoop.sh
```

##### 8. Stop/Remove all
```cmd
docker stop $(docker ps -a -q)
docker rm $(docker ps -a -q)
```

##### 9. Aplikacija v Javi, ki se požene na Hadoop gruči ter uporabi MapReducer 
Izdelati morate aplikacijo v Javi, ki jo boste naložili na glavni strežnik (master) ter se bo izvedla na načinu MapReduce.

Za ta namen naredite Java Maven Projekt v programsko razvojnem okolju (Intellij, Eclipse...) , kjer morate samemu projektu dodati Maven odvnisnosti za podporo MapReduce izvedbi aplikacije.

V datoteko s podatki o Maven repozitorijih (pom.xml) dodamo repozitorij za Hadoop Common in Hadoop MapReduce Client Core:
```xml
    <dependencies>
        <dependency>
            <groupId>org.apache.hadoop</groupId>
            <artifactId>hadoop-common</artifactId>
            <version>2.7.2</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.hadoop</groupId>
            <artifactId>hadoop-mapreduce-client-core</artifactId>
            <version>2.7.2</version>
        </dependency>
    </dependencies>
```

Izvedba testnega programa:
V direktorij "/src/main/java" dodamo nov razred z imenom "SodaLiha" ter kopiramo naslednjo kodo.

```java
package si.gemma;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.conf.Configuration;


public class SodaLiha {
    public static class PreverjanjeSodostiLihosti extends Mapper<Object, Text, Text, IntWritable>{

        private final static IntWritable one = new IntWritable(1);

        private Text besedaSoda = new Text("Soda");
        private Text besedaLiha = new Text("Liha");

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            StringTokenizer itr = new StringTokenizer(value.toString());
            while (itr.hasMoreTokens()) {
                 int stevilka = Integer.parseInt(itr.nextToken());

                 if(stevilka % 2 == 0) // soda
                    context.write(besedaSoda, one);
                 else
                     context.write(besedaLiha, one);
            }
        }
    }

    public static class SestejSodeInLihe extends Reducer<Text,IntWritable,Text,IntWritable> {
        private IntWritable result = new IntWritable();

        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();
        conf.set("mapred.job.queue.name", "default");

        // configuration should contain reference to your namenode
        FileSystem fs = FileSystem.get(new Configuration());
        // true stands for recursively deleting the folder you gave
        fs.delete(new Path(args[1]), true);

        Job job = Job.getInstance(conf, "Sestevek Sodih in lihih stevil");

        job.setJarByClass(SodaLiha.class);
        job.setMapperClass(PreverjanjeSodostiLihosti.class);
        job.setReducerClass(SestejSodeInLihe.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
```

Pred build-anjem projekta mu nastavite lastnost, da bo izhodna datoteka artifakt (pri Intellij dodate Artifakte pod izbiro v meniju "File->Project Structure..."). 

##### 10. Dodajanje datoteke na strežnik (containter) ter nato še na HDFS datotečni sistem Hadoopa
Prijava v glavni strežnik
```cmd
docker exec -it hadoop-master bash
```

Na strežniku kreiramo potrebne poti
```cmd
mkdir app
mkdir testData
```

Odjava iz glavnega strežnika
```cmd
exit
```

Dodajanje datoteke na glavni strežni (container) 
```cmd
docker cp LocalTestData/testnestevilke1 hadoop-master:/root/testData
docker cp LocalTestData/testnestevilke2 hadoop-master:/root/testData
docker cp LocalTestData/SodaLiha.jar hadoop-master:/root/app
```

Prijava v glavni strežnik
```cmd
docker exec -it hadoop-master bash
```

Kreiranje novega direktorija na HDFS datotečnem sistemu
```cmd
hadoop fs -mkdir /user/root/inputData
```

Dodajanje datoteke na HDFS datotečni sistem
```cmd
hadoop fs -mkdir -p inputData
```

```cmd
hdfs dfs -put testData/testnestevilke1 /user/root/inputData
hdfs dfs -put testData/testnestevilke2 /user/root/inputData
```
    
##### 11. Zagon Java Map Reduce aplikacije
Poženite testno aplikacijo štetja besed
```cmd
hadoop jar app/SodaLiha.jar si.gemma.SodaLiha inputData outputData
```

##### 12. Preverjanje delovanja preko spletnih vmesnikov
Delovanje strežnika in zagon aplikacije lahko spremljate na lokalnem omrežju na portu [8088](http://localhost:8088/cluster/apps)

Datotečni sistem vzpostavljene gruče strežnikov lahko vidite na portu [50070](http://localhost:50070/dfshealth.html#tab-overview)

##### 13. Branje izhodnih rezultatov
Poženite testno aplikacijo štetja besed
```cmd
hdfs dfs -cat outputData/part-r-00000
```


P.S. 1:
Programsko orodje Docker potrebuje za delovanje orodje za virtualizacijo (npr. Oracle Virtual Box, Hyper-V...). V primeru, da uporabljate operacijski sistem Windows ter nimate nameščenega nobenega orodja za virtualizaciji, vam bo Docker sam ponudil možnost uporabe Hyper-V orodja, ki je podprt od Windows 7 (SP1) naprej.

P.S. 2:
V kolikor pri uporabi Docker-ja prihaja do napake pri povezavi z repozitoriji (Timeout napaka - napako se lahko reproducira z ukazom "docker run hello-world"), poskusite napako rešiti, da prenesete Docker Edge verzijo. [Docker](https://docs.docker.com/docker-for-windows/install/#download-docker-for-windows)

##### Java
[IntelliJ](https://www.jetbrains.com/idea/download/#section=windows)
[Stackoverflow](https://stackoverflow.com/questions/29268845/running-mapreduce-remotely)
