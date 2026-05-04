import { StyleSheet, Text, View } from "react-native";

type StatusBadgeProps = {
  label: string;
  tone?: "good" | "warning" | "neutral" | "danger";
};

export function StatusBadge({ label, tone = "neutral" }: StatusBadgeProps) {
  return (
    <View style={[styles.badge, styles[tone]]}>
      <Text style={[styles.text, styles[`${tone}Text`]]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    alignSelf: "flex-start",
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 5
  },
  text: {
    fontSize: 12,
    fontWeight: "800"
  },
  good: {
    backgroundColor: "#e5f6ed"
  },
  goodText: {
    color: "#1d7a47"
  },
  warning: {
    backgroundColor: "#fff3d7"
  },
  warningText: {
    color: "#8a5a00"
  },
  neutral: {
    backgroundColor: "#e8f3f2"
  },
  neutralText: {
    color: "#1f6f73"
  },
  danger: {
    backgroundColor: "#fde8e7"
  },
  dangerText: {
    color: "#b42318"
  }
});
