import React, {useEffect} from 'react';
import {Provider} from 'react-redux';
import {PersistGate} from 'redux-persist/integration/react';
import {NavigationContainer} from '@react-navigation/native';
import {SafeAreaProvider} from 'react-native-safe-area-context';
import {store, persistor} from './src/store';
import MainNavigator from './src/navigation/MainNavigator';
import {requestPermissions} from './src/utils/permissions';
import {initializeServices} from './src/services';
import SplashScreen from './src/screens/SplashScreen';

const App: React.FC = () => {
  useEffect(() => {
    const init = async () => {
      await requestPermissions();
      await initializeServices();
    };
    init();
  }, []);

  return (
    <Provider store={store}>
      <PersistGate loading={<SplashScreen />} persistor={persistor}>
        <SafeAreaProvider>
          <NavigationContainer>
            <MainNavigator />
          </NavigationContainer>
        </SafeAreaProvider>
      </PersistGate>
    </Provider>
  );
};

export default App;