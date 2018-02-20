### It is an android library to easily multithread heavy tasks ###

This generic RxJava pattern allows to emit some data and do heavy calculations in parallel threads. 
After the computation result data will be reduced to one thread. Best used when data could be divided in 8-16 parts

### How do I get set up? ###

Build the library to get AAR files first.
Then in your project click File > New > New Module. Click Import .JAR/.AAR Package then click Next. Enter the location of the compiled AAR or JAR file then click Finish.
Click File > Project Structure > Modules > app > Dependencies > + > Module Dependency > HeavyThreading

You can see sample usage and some dummy test code in "App" module of this repo.