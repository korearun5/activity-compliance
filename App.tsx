import { NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { useEffect, useState } from "react";
import { StatusBar } from "react-native";

import { AdminHomeScreen } from "./src/screens/AdminHomeScreen";
import {
  getSavedRole,
  getSavedUsername,
  login,
  logout,
  Role
} from "./src/auth/authService";
import { LoginScreen } from "./src/screens/LoginScreen";
import { RootStackParamList } from "./src/navigation/types";
import { UserHomeScreen } from "./src/screens/UserHomeScreen";

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function App() {
  const [role, setRole] = useState<Role | null>(null);
  const [username, setUsername] = useState<string | null>(null);
  const [isCheckingSession, setIsCheckingSession] = useState(true);

  useEffect(() => {
    async function restoreSession() {
      const savedRole = await getSavedRole();
      const savedUsername = await getSavedUsername();
      setRole(savedRole);
      setUsername(savedUsername);
      setIsCheckingSession(false);
    }

    restoreSession();
  }, []);

  async function handleLogin(username: string, password: string) {
    const signedInRole = await login(username, password);
    setRole(signedInRole);
    setUsername(username);
  }

  async function handleLogout() {
    await logout();
    setRole(null);
    setUsername(null);
  }

  if (isCheckingSession) {
    return null;
  }

  const staffRole = isStaffRole(role) ? role : null;

  return (
    <>
      <StatusBar barStyle="dark-content" />
      <NavigationContainer>
        <Stack.Navigator screenOptions={{ headerShown: false }}>
          {staffRole ? (
            <Stack.Screen name="AdminHome">
              {() => (
                <AdminHomeScreen currentRole={staffRole} onLogout={handleLogout} />
              )}
            </Stack.Screen>
          ) : role === "farmer" ? (
            <Stack.Screen name="UserHome">
              {() => <UserHomeScreen username={username} onLogout={handleLogout} />}
            </Stack.Screen>
          ) : (
            <Stack.Screen name="Login">
              {() => <LoginScreen onLogin={handleLogin} />}
            </Stack.Screen>
          )}
        </Stack.Navigator>
      </NavigationContainer>
    </>
  );
}

function isStaffRole(role: Role | null): role is Exclude<Role, "farmer"> {
  return role === "admin" || role === "fpoManager" || role === "fieldCoordinator";
}
