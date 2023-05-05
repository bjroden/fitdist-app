# Fitdist-app

This application utilizes the
[fitdist-kotlin](https://github.com/bjroden/fitdist-kotlin) 
library to fit data (formatted in the same format used 
[Arena's Input Analyzer](https://www.rockwellautomation.com/en-us/products/software/arena-simulation.html))
to distributions used in our sponsor's 
[Kotlin Simulation Library](https://github.com/rossetti/KSL). 
We assign a score to each distribution using the Chi-Squared and
Kolmogorov-Smirnov statistical tests.

## Running
---
This project uses the [gradle](https://gradle.org/) build system. If you have
gradle installed, either manually or through an IDE, that can be used for
building and testing the application. Alternatively, the gradlew script can be
used without installing gradle (gradlew.bat on windows, gradlew on *nix).

If using gradlew, the application can be built with:

    ./gradlew build

## Usage
---
1. Import data by using the File dropdown menu at the top left of the
   application and clicking "Input Data"
    - At this point in time, the application only accepts data formatted
      similarly to Arena's Input Analyzer. Input Analyzer only takes a newline
      separated list of values.
2. Select the distributions you would like to fit your data to from the left
   side of the screen
3. Configure the bin width
    - This bin width is used for the Chi-Squared calculations as well as the
      Histogram visualization once the run button is hit.
4. Configure the Chi-squared and Kolmogorov-Smirnov tests (referred to as K-S
   test from here on)
    - You can enable or disable either test by clicking the checkmark (if you
      don't want to calculate these tests)
    - You can change the weights for each statistical test. This is if you have
      a preference
