import { useState } from "react";
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View
} from "react-native";

import { getErrorMessage } from "../core/errors/AppError";
import { UserProfileInput } from "../core/model/types";

type LoginScreenProps = {
  onLogin: (username: string, password: string) => Promise<void>;
  onSignup: (data: {
    username: string;
    password: string;
    profile: UserProfileInput;
  }) => Promise<void>;
};

type AuthMode = "login" | "signup";

export function LoginScreen({ onLogin, onSignup }: LoginScreenProps) {
  const [mode, setMode] = useState<AuthMode>("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [region, setRegion] = useState("");
  const [village, setVillage] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);

  async function handleSubmit() {
    const trimmedUsername = username.trim();

    if (!trimmedUsername || !password) {
      setError("Enter username and password.");
      return;
    }

    if (mode === "signup" && (!name.trim() || !phone.trim() || !region.trim() || !village.trim())) {
      setError("Enter name, phone, region, and village.");
      return;
    }

    setIsSubmitting(true);
    setError("");

    try {
      if (mode === "signup") {
        await onSignup({
          username: trimmedUsername,
          password,
          profile: {
            displayName: name.trim(),
            locationName: region.trim(),
            phone: phone.trim(),
            siteName: village.trim()
          }
        });
      } else {
        await onLogin(trimmedUsername, password);
      }

      setPassword("");
    } catch (authError) {
      setError(getErrorMessage(authError, "Unable to continue."));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <SafeAreaView style={styles.screen}>
      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : undefined}
        style={styles.keyboardView}
      >
        <ScrollView contentContainerStyle={styles.scrollContent}>
          <View style={styles.shell}>
            <Text style={styles.eyebrow}>
              {mode === "signup" ? "Farmer signup" : "Welcome"}
            </Text>
            <Text style={styles.title}>
              {mode === "signup" ? "Create Farmer Account" : "Role Login"}
            </Text>
            <Text style={styles.copy}>
              {mode === "signup"
                ? "Create a local farmer profile. Later this same flow will connect to Spring Boot."
                : "Try admin/admin123, user/user123, or a farmer account you created."}
            </Text>

            <View style={styles.modeRow}>
              <Pressable
                style={[
                  styles.modeButton,
                  mode === "login" && styles.modeButtonActive
                ]}
                onPress={() => {
                  setMode("login");
                  setError("");
                }}
              >
                <Text
                  style={[
                    styles.modeButtonText,
                    mode === "login" && styles.modeButtonTextActive
                  ]}
                >
                  Login
                </Text>
              </Pressable>
              <Pressable
                style={[
                  styles.modeButton,
                  mode === "signup" && styles.modeButtonActive
                ]}
                onPress={() => {
                  setMode("signup");
                  setError("");
                }}
              >
                <Text
                  style={[
                    styles.modeButtonText,
                    mode === "signup" && styles.modeButtonTextActive
                  ]}
                >
                  Farmer signup
                </Text>
              </Pressable>
            </View>

            <View style={styles.form}>
              {mode === "signup" ? (
                <>
                  <Field label="Farmer name" value={name} onChange={setName} />
                  <Field
                    label="Phone"
                    value={phone}
                    onChange={setPhone}
                    keyboardType="phone-pad"
                  />
                  <Field label="Region" value={region} onChange={setRegion} />
                  <Field label="Village" value={village} onChange={setVillage} />
                </>
              ) : null}

              <Field
                label="Username"
                value={username}
                onChange={setUsername}
                placeholder={mode === "signup" ? "Create username" : "admin or user"}
              />

              <View style={styles.field}>
                <Text style={styles.label}>Password</Text>
                <View style={styles.passwordRow}>
                  <TextInput
                    onChangeText={setPassword}
                    onSubmitEditing={handleSubmit}
                    placeholder={
                      mode === "signup" ? "Create password" : "Enter password"
                    }
                    secureTextEntry={!isPasswordVisible}
                    style={styles.passwordInput}
                    value={password}
                  />
                  <Pressable
                    style={({ pressed }) => [
                      styles.passwordToggle,
                      pressed && styles.passwordTogglePressed
                    ]}
                    onPress={() => setIsPasswordVisible((visible) => !visible)}
                  >
                    <Text style={styles.passwordToggleText}>
                      {isPasswordVisible ? "Hide" : "Show"}
                    </Text>
                  </Pressable>
                </View>
              </View>

              {error ? <Text style={styles.error}>{error}</Text> : null}

              <Pressable
                disabled={isSubmitting}
                style={({ pressed }) => [
                  styles.primaryButton,
                  (pressed || isSubmitting) && styles.primaryButtonPressed
                ]}
                onPress={handleSubmit}
              >
                <Text style={styles.primaryButtonText}>
                  {isSubmitting
                    ? "Please wait..."
                    : mode === "signup"
                      ? "Create account"
                      : "Log in"}
                </Text>
              </Pressable>
            </View>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function Field({
  label,
  value,
  onChange,
  placeholder,
  keyboardType
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  keyboardType?: "default" | "phone-pad";
}) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        autoCapitalize="none"
        autoCorrect={false}
        keyboardType={keyboardType}
        onChangeText={onChange}
        placeholder={placeholder ?? label}
        style={styles.input}
        value={value}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#f4f7fb"
  },
  keyboardView: {
    flex: 1
  },
  scrollContent: {
    flexGrow: 1
  },
  shell: {
    width: "100%",
    maxWidth: 460,
    alignSelf: "center",
    paddingHorizontal: 24,
    paddingVertical: 36,
    flex: 1,
    justifyContent: "center"
  },
  eyebrow: {
    color: "#356b6f",
    fontSize: 14,
    fontWeight: "700",
    marginBottom: 10,
    textTransform: "uppercase"
  },
  title: {
    color: "#172126",
    fontSize: 34,
    fontWeight: "800",
    marginBottom: 12
  },
  copy: {
    color: "#53666f",
    fontSize: 16,
    lineHeight: 24,
    marginBottom: 22
  },
  modeRow: {
    backgroundColor: "#e8eef2",
    borderRadius: 8,
    flexDirection: "row",
    gap: 4,
    marginBottom: 18,
    padding: 4
  },
  modeButton: {
    alignItems: "center",
    borderRadius: 6,
    flex: 1,
    minHeight: 42,
    justifyContent: "center"
  },
  modeButtonActive: {
    backgroundColor: "#ffffff"
  },
  modeButtonText: {
    color: "#53666f",
    fontSize: 14,
    fontWeight: "800"
  },
  modeButtonTextActive: {
    color: "#172126"
  },
  form: {
    gap: 14
  },
  field: {
    gap: 8
  },
  label: {
    color: "#24343b",
    fontSize: 14,
    fontWeight: "700"
  },
  input: {
    minHeight: 50,
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    backgroundColor: "#ffffff",
    color: "#172126",
    fontSize: 16,
    paddingHorizontal: 14
  },
  passwordRow: {
    minHeight: 50,
    borderColor: "#c9d7df",
    borderRadius: 8,
    borderWidth: 1,
    backgroundColor: "#ffffff",
    flexDirection: "row",
    alignItems: "center"
  },
  passwordInput: {
    minHeight: 50,
    color: "#172126",
    flex: 1,
    fontSize: 16,
    paddingHorizontal: 14
  },
  passwordToggle: {
    minHeight: 44,
    minWidth: 62,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 3
  },
  passwordTogglePressed: {
    opacity: 0.64
  },
  passwordToggleText: {
    color: "#1f6f73",
    fontSize: 14,
    fontWeight: "800"
  },
  error: {
    color: "#b42318",
    fontSize: 14,
    fontWeight: "600"
  },
  primaryButton: {
    minHeight: 52,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 8,
    backgroundColor: "#1f6f73",
    marginTop: 4
  },
  primaryButtonPressed: {
    opacity: 0.78
  },
  primaryButtonText: {
    color: "#ffffff",
    fontSize: 16,
    fontWeight: "800"
  }
});
