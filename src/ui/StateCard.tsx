import { StyleSheet, Text, View } from "react-native";

type StateTone = "empty" | "error" | "info" | "success" | "warning";

type StateCardProps = {
  message: string;
  title?: string;
  tone?: StateTone;
};

export function StateCard({ message, title, tone = "info" }: StateCardProps) {
  return (
    <View style={[styles.card, toneStyles[tone]]}>
      {title ? <Text style={styles.title}>{title}</Text> : null}
      <Text style={styles.message}>{message}</Text>
    </View>
  );
}

const toneStyles: Record<StateTone, object> = {
  empty: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e4ea"
  },
  error: {
    backgroundColor: "#fff1f0",
    borderColor: "#ffc9c4"
  },
  info: {
    backgroundColor: "#f7fafb",
    borderColor: "#d9e4ea"
  },
  success: {
    backgroundColor: "#edf8f3",
    borderColor: "#b8dec9"
  },
  warning: {
    backgroundColor: "#fff8e8",
    borderColor: "#f0d38a"
  }
};

const styles = StyleSheet.create({
  card: {
    borderRadius: 8,
    borderWidth: 1,
    gap: 6,
    padding: 14
  },
  message: {
    color: "#53666f",
    fontSize: 14,
    fontWeight: "700",
    lineHeight: 20
  },
  title: {
    color: "#172126",
    fontSize: 15,
    fontWeight: "800"
  }
});
