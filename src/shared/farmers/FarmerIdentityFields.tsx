import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";

import {
  FARMER_CATEGORIES,
  FARMER_GENDERS,
  FarmerIdentityInput,
  formatFarmerCodeLabel
} from "./farmerIdentity";

type FarmerIdentityFieldsProps = {
  includeLogin: boolean;
  onChange: (value: FarmerIdentityInput) => void;
  value: FarmerIdentityInput;
};

type FarmerIdentityFieldKey = keyof FarmerIdentityInput;

export function FarmerIdentityFields({
  includeLogin,
  onChange,
  value
}: FarmerIdentityFieldsProps) {
  function updateField(key: FarmerIdentityFieldKey, nextValue: string) {
    onChange({ ...value, [key]: nextValue });
  }

  return (
    <View style={styles.block}>
      <View style={styles.grid}>
        <FarmerField
          label="Member number"
          value={value.memberNumber}
          onChange={(nextValue) => updateField("memberNumber", nextValue)}
        />
        <FarmerField
          autoCapitalize="words"
          label="Full name"
          value={value.displayName}
          onChange={(nextValue) => updateField("displayName", nextValue)}
        />
        <FarmerField
          keyboardType="phone-pad"
          label="Mobile number"
          value={value.mobileNumber}
          onChange={(nextValue) => updateField("mobileNumber", nextValue)}
        />
        <FarmerField
          keyboardType="phone-pad"
          label="Alternate mobile"
          value={value.alternateMobileNumber}
          onChange={(nextValue) => updateField("alternateMobileNumber", nextValue)}
        />
        <FarmerField
          keyboardType="numeric"
          label="Aadhaar number"
          value={value.aadhaarNumber}
          onChange={(nextValue) => updateField("aadhaarNumber", nextValue)}
        />
        <FarmerField
          label="Village"
          value={value.village}
          onChange={(nextValue) => updateField("village", nextValue)}
        />
        <FarmerField
          label="Taluka"
          value={value.taluka}
          onChange={(nextValue) => updateField("taluka", nextValue)}
        />
        <FarmerField
          label="District"
          value={value.districtName}
          onChange={(nextValue) => updateField("districtName", nextValue)}
        />
        <FarmerField
          label="State"
          value={value.stateName}
          onChange={(nextValue) => updateField("stateName", nextValue)}
        />
        <FarmerField
          keyboardType="numeric"
          label="Age"
          value={value.age}
          onChange={(nextValue) => updateField("age", nextValue)}
        />
        {includeLogin ? (
          <>
            <FarmerField
              label="Username"
              value={value.username}
              onChange={(nextValue) => updateField("username", nextValue)}
            />
            <FarmerField
              label="Password"
              secureTextEntry
              value={value.password}
              onChange={(nextValue) => updateField("password", nextValue)}
            />
          </>
        ) : null}
      </View>

      <FarmerOptionGroup
        label="Gender"
        options={FARMER_GENDERS}
        value={value.gender}
        onChange={(nextValue) => updateField("gender", nextValue)}
      />
      <FarmerOptionGroup
        label="Farmer category"
        options={FARMER_CATEGORIES}
        value={value.farmerCategory}
        onChange={(nextValue) => updateField("farmerCategory", nextValue)}
      />
    </View>
  );
}

function FarmerField({
  autoCapitalize = "none",
  keyboardType,
  label,
  onChange,
  secureTextEntry,
  value
}: {
  autoCapitalize?: "none" | "words";
  keyboardType?: "decimal-pad" | "number-pad" | "numeric" | "phone-pad";
  label: string;
  onChange: (value: string) => void;
  secureTextEntry?: boolean;
  value: string;
}) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        autoCapitalize={autoCapitalize}
        autoCorrect={false}
        keyboardType={keyboardType}
        placeholder={label}
        placeholderTextColor="#8a99a1"
        secureTextEntry={secureTextEntry}
        style={styles.input}
        value={value}
        onChangeText={onChange}
      />
    </View>
  );
}

function FarmerOptionGroup<T extends string>({
  label,
  onChange,
  options,
  value
}: {
  label: string;
  onChange: (value: T) => void;
  options: readonly T[];
  value: string;
}) {
  return (
    <View style={styles.optionBlock}>
      <Text style={styles.label}>{label}</Text>
      <View style={styles.optionRow}>
        {options.map((option) => {
          const isSelected = option === value;

          return (
            <Pressable
              accessibilityRole="button"
              key={option}
              style={[styles.optionButton, isSelected && styles.optionButtonActive]}
              onPress={() => onChange(option)}
            >
              <Text
                style={[
                  styles.optionButtonText,
                  isSelected && styles.optionButtonTextActive
                ]}
              >
                {formatFarmerCodeLabel(option)}
              </Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  block: {
    gap: 12
  },
  field: {
    flex: 1,
    gap: 6,
    minWidth: 190
  },
  grid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10
  },
  input: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d8df",
    borderRadius: 8,
    borderWidth: 1,
    color: "#172126",
    fontSize: 14,
    fontWeight: "700",
    minHeight: 42,
    paddingHorizontal: 12,
    paddingVertical: 9
  },
  label: {
    color: "#53666f",
    fontSize: 12,
    fontWeight: "800"
  },
  optionBlock: {
    gap: 6
  },
  optionButton: {
    backgroundColor: "#ffffff",
    borderColor: "#c9d8df",
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 10,
    paddingVertical: 8
  },
  optionButtonActive: {
    backgroundColor: "#1f6f73",
    borderColor: "#1f6f73"
  },
  optionButtonText: {
    color: "#53666f",
    fontSize: 12,
    fontWeight: "800"
  },
  optionButtonTextActive: {
    color: "#ffffff"
  },
  optionRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8
  }
});
