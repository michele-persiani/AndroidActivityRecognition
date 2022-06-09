# AndroidActivityRecognition



Android application that records and saves the data from the available sensors.


Main components:
- **MainActivity**: for testing purposes the activity has buttons to utilize the main components of the application. The final version will remove the activity and keep only the functionalities from RecordService.
- **RecordService**: main service that records and saves sensor data
  - **SensorAccumulatorManager**: manager handling the sensors and their accumulation into dataframes
  - **Persistance**: class for saving on external files the current state of SensorAccumulatorManager
  - **RecurrentSave**: this component recurrently saves RecordService readings
- **TensorflowLiteModels**: Singleton tensorflow models that uses sensors
  - **SOM**: Self Orgaizing Map (SOM) showing the classification of the sensor data. Trained with a Ticwatch 3.
- **ActivityWatchFace**: watchface to be utilized on a watch. The watchface shows the current results of the activity classifier. 
- **SOMWatchface**: watchface to show the SOM.
- **DialogflowService**: service to connect to a Dialogflow chatbot.
  -**SpeechChatbot**: chatbot that uses voice to communicate with the user. Supports translation to multiple language (its done through Google Translate so the quality is low)
  -**ChatbotActivity**, **ChatbotActivity**: Activity and watchface to access the chatbot


Author: **Michele Persiani**, michelep@cs.umu.se

**Copiright of Umea University**.
