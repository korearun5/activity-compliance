export const FARMER_GENDERS = ["MALE", "FEMALE", "OTHER"] as const;

export const FARMER_CATEGORIES = [
  "MARGINAL",
  "SMALL",
  "SEMI_MEDIUM",
  "MEDIUM",
  "LARGE"
] as const;

export type FarmerGender = (typeof FARMER_GENDERS)[number];
export type FarmerCategory = (typeof FARMER_CATEGORIES)[number];

export type FarmerIdentityInput = {
  aadhaarNumber: string;
  age: string;
  alternateMobileNumber: string;
  displayName: string;
  districtName: string;
  farmerCategory: string;
  gender: string;
  memberNumber: string;
  mobileNumber: string;
  password: string;
  stateName: string;
  taluka: string;
  username: string;
  village: string;
};

export type FarmerIdentityValidationOptions = {
  requireLogin: boolean;
};

export function emptyFarmerIdentityInput(
  overrides: Partial<FarmerIdentityInput> = {}
): FarmerIdentityInput {
  return {
    aadhaarNumber: "",
    age: "",
    alternateMobileNumber: "",
    displayName: "",
    districtName: "",
    farmerCategory: FARMER_CATEGORIES[0],
    gender: FARMER_GENDERS[0],
    memberNumber: "",
    mobileNumber: "",
    password: "",
    stateName: "Maharashtra",
    taluka: "",
    username: "",
    village: "",
    ...overrides
  };
}

export function normalizeFarmerIdentityInput(
  input: FarmerIdentityInput
): FarmerIdentityInput {
  return {
    aadhaarNumber: input.aadhaarNumber.trim(),
    age: input.age.trim(),
    alternateMobileNumber: input.alternateMobileNumber.trim(),
    displayName: input.displayName.trim(),
    districtName: input.districtName.trim(),
    farmerCategory: input.farmerCategory.trim(),
    gender: input.gender.trim(),
    memberNumber: input.memberNumber.trim(),
    mobileNumber: normalizeIndianMobileDigits(input.mobileNumber) || input.mobileNumber.trim(),
    password: input.password,
    stateName: input.stateName.trim(),
    taluka: input.taluka.trim(),
    username: input.username.trim(),
    village: input.village.trim()
  };
}

export function validateFarmerIdentityInput(
  input: FarmerIdentityInput,
  options: FarmerIdentityValidationOptions
) {
  if (
    !input.memberNumber ||
    !input.displayName ||
    !input.mobileNumber ||
    !input.village ||
    !input.taluka ||
    !input.districtName ||
    !input.stateName ||
    !input.gender ||
    !input.farmerCategory ||
    (options.requireLogin && (!input.username || !input.password))
  ) {
    return options.requireLogin
      ? "Enter member number, full name, mobile, village, taluka, district, state, gender, category, username, and password."
      : "Enter member number, full name, mobile, village, taluka, district, state, gender, and category.";
  }

  if (!normalizeIndianMobileDigits(input.mobileNumber)) {
    return "Mobile number must be a 10 digit Indian mobile number.";
  }

  if (!FARMER_GENDERS.includes(input.gender as FarmerGender)) {
    return "Gender must be Male, Female, or Other.";
  }

  if (!FARMER_CATEGORIES.includes(input.farmerCategory as FarmerCategory)) {
    return "Farmer category must be Marginal, Small, Semi-medium, Medium, or Large.";
  }

  if (!isValidOptionalAadhaar(input.aadhaarNumber)) {
    return "Aadhaar number must be 12 digits when provided.";
  }

  if (!isValidOptionalAge(input.age)) {
    return "Age must be a whole number from 0 to 120.";
  }

  if (options.requireLogin && input.password.length < 8) {
    return "Password must be at least 8 characters.";
  }

  return "";
}

export function normalizeIndianMobileDigits(value: string) {
  let digits = value.replace(/\D/g, "");
  if (digits.length === 12 && digits.startsWith("91")) {
    digits = digits.slice(2);
  }
  return /^[6-9][0-9]{9}$/.test(digits) ? digits : "";
}

export function isValidOptionalAadhaar(aadhaarNumber: string | undefined) {
  if (!aadhaarNumber) {
    return true;
  }

  return /^[0-9]{12}$/.test(aadhaarNumber.replace(/\D/g, ""));
}

export function isValidOptionalAge(age: string | undefined) {
  if (!age) {
    return true;
  }

  const parsedAge = Number(age);
  return Number.isInteger(parsedAge) && parsedAge >= 0 && parsedAge <= 120;
}

export function formatFarmerCodeLabel(value: string) {
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}
