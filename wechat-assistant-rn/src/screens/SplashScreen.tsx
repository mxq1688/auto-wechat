import React, {useEffect} from 'react';
import {
  View,
  Text,
  StyleSheet,
  ActivityIndicator,
  Image,
} from 'react-native';

const SplashScreen: React.FC = () => {
  return (
    <View style={styles.container}>
      <View style={styles.logoContainer}>
        <View style={styles.logo}>
          <Text style={styles.logoText}>微信</Text>
          <Text style={styles.logoText}>助手</Text>
        </View>
      </View>
      <ActivityIndicator size="large" color="#07C160" style={styles.loader} />
      <Text style={styles.loadingText}>加载中...</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
    justifyContent: 'center',
    alignItems: 'center',
  },
  logoContainer: {
    marginBottom: 50,
  },
  logo: {
    width: 120,
    height: 120,
    backgroundColor: '#07C160',
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
  },
  logoText: {
    color: '#FFFFFF',
    fontSize: 24,
    fontWeight: 'bold',
  },
  loader: {
    marginTop: 30,
  },
  loadingText: {
    marginTop: 10,
    fontSize: 14,
    color: '#999999',
  },
});

export default SplashScreen;