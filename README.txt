PREFORMANCE MEASUREMENTS
Using the bash script (for 3 trials per each compression program)
Tests are run with default program arguments/settings:

input=/usr/local/cs/graalvm-ce-java17-22.3.1/lib/modules
time gzip <$input >gzip.gz
time pigz <$input >pigz.gz
time java Pigzj <$input >Pigzj.gz
time ./pigzj <$input >pigzj.gz
ls -l gzip.gz pigz.gz Pigzj.gz pigzj.gz

DATA COLLECTED
gzip pigz Pigzj pigzj Trail #1
real    0m9.906s
user    0m9.109s
sys     0m0.108s
$ time pigz <$input >pigz.gz

real    0m2.993s
user    0m9.207s
sys     0m0.072s
$ time java Pigzj <$input >Pigzj.gz

real    0m3.484s
user    0m9.502s
sys     0m0.446s
$ time ./pigzj <$input >pigzj.gz

real    0m3.310s
user    0m9.449s
sys     0m0.498s
$ ls -l gzip.gz pigz.gz Pigzj.gz pigzj.gz




gzip pigz Pigzj pigzj Trail #2
real    0m9.711s
user    0m9.103s
sys     0m0.111s
$ time pigz <$input >pigz.gz

real    0m3.129s
user    0m9.654s
sys     0m0.084s
$ time java Pigzj <$input >Pigzj.gz

real    0m3.387s
user    0m9.465s
sys     0m0.416s
$ time ./pigzj <$input >pigzj.gz

real    0m3.309s
user    0m9.507s
sys     0m0.532s
$ ls -l gzip.gz pigz.gz Pigzj.gz pigzj.gz




gzip pigz Pigzj pigzj Trail #3
real    0m9.439s
user    0m9.136s
sys     0m0.083s
$ time pigz <$input >pigz.gz

real    0m2.959s
user    0m9.217s
sys     0m0.057s
$ time java Pigzj <$input >Pigzj.gz

real    0m3.348s
user    0m9.424s
sys     0m0.429s
$ time ./pigzj <$input >pigzj.gz

real    0m3.441s
user    0m9.447s
sys     0m0.526s
$ ls -l gzip.gz pigz.gz Pigzj.gz pigzj.gz




All user times seem to be fairly close to each other.PIGZ seems to have the fastest real time
followed closely by PIGZJ and then GZIP which tends to have more than double the real time. The
user time of all trials seem to be fairly consistent over all three types of compression program.
PIGZ tends to have the best sys time, followed by GZIP and then PIGZJ.

TESTING PIGZJ AND PIGZ PERFORMANCE WITH DIFFERENT PROCESSOR VALUES:
Three trials in total with one, two and four processors for each compression call.
The Original file input file /usr/local/cs/jdk-17.0.1/lib/modules has 126421170 bytes.
PIGZJ compresses modules into 43210207 bytes at a ratio of 1:0.3417.
 PIGZ compresses modules into 43261121 bytes at a ratio of 1:0.3421.

(PIGZJ -P 1)
real    0m8.047s
user    0m6.812s
sys     0m0.733s

(PIGZ -P 1)
real    0m10.330s
user    0m6.946s
sys     0m0.080s

(PIGZJ -P 2)
real    0m4.124s
user    0m7.210s
sys     0m0.229s

(PIGZ -P 2)
real    0m8.338s
user    0m6.988s
sys     0m0.196s

(PIGZJ -P 4)
real    0m3.095s
user    0m6.889s
sys     0m0.740s

(PIGZ -P 4)
real    0m6.038s
user    0m6.854s
sys     0m0.415s

For all trials of different number of set processors, PIGZJ seems to have a better real time,
the user seems close or near each other in each trial and for sys time PIGZ tends to have a 
faster time. Overall, increasing the processors decreases the time as expected since increasing
the processors increases the ammount of threads that are allowed to run compression in parrallel,
as a result decreasing the total runtime of the program. The compression ratios of both PIGZ and
PIGZJ seem to be very close to each other as well.

STRACE ANALYSIS ON GZIP PIGZ AND PIGZJ
The majoirty of system calls for GZIP where read and write calls. PIGZ also made many read and 
write calls however it seemed like there were less readn and write calls. It was also noticible
how PIGZ would make more system calls having to do with memory allocation/dealocation and for
memory management in general as well as other supporting system calls. Many futex calls are made
by PIGZ as well as they are required for blocking and thread shared-memory synchronization. PIGZJ
makes calls simmillar to PIGZ, again having noticbly less read and write calls than GZIP. PIGZJ
also has a mix of other memory management system calls as well as futex in order to block for
threads and synchronization. It would seem that simply only making a bulk ammount of read and write
calls adds up to a much larger runtime. PIGZ's and PIGZJ's use of futex for multiple threads to
run allows each of their programs to make less read and write calls therby drastically decreasing the
runtime.

SUMMARY
The actual compression task of the program really seems to be the bottleneck of the compression program's
performance, simply by using a multithreaded approach for the compression part of the program the runtime
is drastically reduced. This increase in performance is showned by a real time comparison where GZIP takes
more than double the ammount of time that PIGZ and PIGZJ takes for the compression. The difference in performance
is further shown in how increasing the number of processors for the PIGZ PIGZJ further reduces the runtime for
the compression program. In terms of system calls, through the use of strace, PIGZ and PIGZJ achieve better
runtimes due to the use of multiple threads which by making more futex calls for synchronization PIGZ and 
PIGZJ are able to make less read and write calls. It is expected as the data shows that PIGZ and PIGZJ will
have greater performance incomparison to GZIP as the filesize and number of processors available increases.
Internally, GZIP will be forced to make more and more read and write calls furhter slowing it down while
PIGZ and PIGZJ can take a multithreaded approach where less read/write calls are necessary in comparison.

IMPLEMENTATION REFERENCES
Some ideas of the block structure where threads can access pages of bytes were influeced by MessAdmin's 
compress program design.
