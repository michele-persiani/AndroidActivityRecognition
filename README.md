# AndroidActivityRecognition



Android application that records and saves the data from the available sensor. 
Work in progress: add Tensorflow Lite classifier

Main Components:
- MainActivity
- RecordServcie: main service that records and saves sensor data
  - RecordServiceStarter: starter for the service

  - SensorAccumulatorManager: manager handling the sensors and their accumulation
  - Persistance: class for saving on external files the current state of SensorAccumulatorManager



Author: **Michele Persiani**, michelep@cs.umu.se

**Copiright of Umea University**
