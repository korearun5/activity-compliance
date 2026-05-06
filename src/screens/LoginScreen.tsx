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

type LoginScreenProps = {
  onLogin: (username: string, password: string) => Promise<void>;
};

export function LoginScreen({ onLogin }: LoginScreenProps) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);

  async function handleSubmit() {
    const trimmedUsername = username.trim();

    if (!trimmedUsername || !password) {
      setError("Enter username and password.");
      return;
    }

    setIsSubmitting(true);
    setError("");

    try {
      await onLogin(trimmedUsername, password);
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
            <Text style={styles.eyebrow}>Welcome</Text>
            <Text style={styles.title}>Sign in</Text>
            <Text style={styles.copy}>
              Use the username and password assigned by your administrator.
            </Text>

            <View style={styles.form}>
              <Field
                label="Username"
                value={username}
                onChange={setUsername}
                placeholder="Enter username"
              />

              <View style={styles.field}>
                <Text style={styles.label}>Password</Text>
                <View style={styles.passwordRow}>
                  <TextInput
                    onChangeText={setPassword}
                    onSubmitEditing={handleSubmit}
                    placeholder="Enter password"
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
                  {isSubmitting ? "Please wait..." : "Log in"}
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
