import {configureStore} from '@reduxjs/toolkit';
import {persistStore, persistReducer} from 'redux-persist';
import AsyncStorage from '@react-native-async-storage/async-storage';
import autoReplyReducer from './slices/autoReplySlice';
import messageReducer from './slices/messageSlice';
import settingsReducer from './slices/settingsSlice';
import callReducer from './slices/callSlice';

const persistConfig = {
  key: 'root',
  storage: AsyncStorage,
  whitelist: ['settings', 'autoReply'],
};

const rootReducer = {
  autoReply: persistReducer({
    ...persistConfig,
    key: 'autoReply',
  }, autoReplyReducer),
  messages: messageReducer,
  settings: persistReducer({
    ...persistConfig,
    key: 'settings',
  }, settingsReducer),
  call: callReducer,
};

export const store = configureStore({
  reducer: rootReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: ['persist/PERSIST', 'persist/REHYDRATE'],
      },
    }),
});

export const persistor = persistStore(store);

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;