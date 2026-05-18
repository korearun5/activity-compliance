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
import {
  resetSessionExpiredNotification,
  subscribeSessionExpired
} from "./src/core/auth/sessionEvents";
import { LoginScreen } from "./src/screens/LoginScreen";
import { RootStackParamList } from "./src/navigation/types";
import { UserHomeScreen } from "./src/screens/UserHomeScreen";
import { ConfirmationModal } from "./src/ui/ConfirmationModal";

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function App() {
  const [role, setRole] = useState<Role | null>(null);
  const [username, setUsername] = useState<string | null>(null);
  const [isCheckingSession, setIsCheckingSession] = useState(true);
  const [isLogoutConfirmVisible, setIsLogoutConfirmVisible] = useState(false);
  const [sessionMessage, setSessionMessage] = useState("");

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

  useEffect(() => {
    return subscribeSessionExpired(async (event) => {
      await logout();
      setRole(null);
      setUsername(null);
      setSessionMessage(event.message);
    });
  }, []);

  async function handleLogin(username: string, password: string) {
    setSessionMessage("");
    const signedInRole = await login(username, password);
    resetSessionExpiredNotification();
    setRole(signedInRole);
    setUsername(username);
  }

  async function handleLogout() {
    await logout();
    setIsLogoutConfirmVisible(false);
    setSessionMessage("");
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
                <AdminHomeScreen
                  currentRole={staffRole}
                  onLogout={() => setIsLogoutConfirmVisible(true)}
                />
              )}
            </Stack.Screen>
          ) : role === "farmer" ? (
            <Stack.Screen name="UserHome">
              {() => (
                <UserHomeScreen
                  username={username}
                  onLogout={() => setIsLogoutConfirmVisible(true)}
                />
              )}
            </Stack.Screen>
          ) : (
            <Stack.Screen name="Login">
              {() => (
                <LoginScreen
                  onLogin={handleLogin}
                  sessionMessage={sessionMessage}
                />
              )}
            </Stack.Screen>
          )}
        </Stack.Navigator>
      </NavigationContainer>
      <ConfirmationModal
        confirmLabel="Log out"
        message="Your current session will close and you will return to login."
        onCancel={() => setIsLogoutConfirmVisible(false)}
        onConfirm={handleLogout}
        title="Log out?"
        visible={isLogoutConfirmVisible}
      />
    </>
  );
}

function isStaffRole(role: Role | null): role is Exclude<Role, "farmer"> {
  return role === "admin" || role === "fpoManager" || role === "fieldCoordinator";
}
