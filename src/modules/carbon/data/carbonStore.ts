export type CarbonLanguage = "English" | "Hindi" | "Marathi";

export type CarbonProfile = {
  aadhaarStatus: "Optional not captured" | "Provided";
  bankStatus: "Linked" | "Pending";
  carbonIdentityId: string;
  croppingPattern: string;
  district: string;
  documents: string[];
  farmerName: string;
  gpsLocation: string;
  id: string;
  irrigationSource: string;
  language: CarbonLanguage;
  livestockCount: number;
  memberNumber: string;
  mobileNumber: string;
  taluka: string;
  tillageStatus: "Conventional" | "No tillage" | "Reduced tillage";
  totalLandHoldingAcres: number;
  village: string;
};

export type SoilProfile = {
  bulkDensity: number;
  carbonPotentialTco2e: number;
  ec: number;
  id: string;
  lastUpdated: string;
  microbialCount: string;
  ndmi: number;
  ndre: number;
  ndvi: number;
  nitrogenKgHa: number;
  phosphorusKgHa: number;
  plotName: string;
  potassiumKgHa: number;
  recommendedInputs: string[];
  soilHealthScore: number;
  soilOrganicCarbonPercent: number;
  texture: string;
  ph: number;
};

export type CarbonActivity = {
  activityScore: number;
  category:
    | "Biological Application"
    | "Compost Addition"
    | "Fertigation"
    | "Harvesting"
    | "Irrigation"
    | "Land Preparation"
    | "Pruning Biomass Incorporation"
    | "Sowing";
  cost: number;
  crop: string;
  date: string;
  emissionReductionTco2e: number;
  evidenceCount: number;
  id: string;
  inputUsed: string;
  laborDays: number;
  quantity: string;
  verificationStatus: "AI pending" | "Expert review" | "Verified";
};

export type CarbonAdvisory = {
  category:
    | "Biological dosage"
    | "Carbon farming practice"
    | "Irrigation advice"
    | "Nutrient deficiency"
    | "Pest issue";
  channel: "In app" | "Voice" | "WhatsApp placeholder";
  createdAt: string;
  id: string;
  language: CarbonLanguage;
  message: string;
  status: "Draft" | "Published";
  title: string;
};

export type CarbonDealer = {
  category: "Biological inputs" | "Carbon farming inputs" | "Soil testing lab";
  distanceKm: number;
  id: string;
  name: string;
  phone: string;
  products: string[];
  rating: number;
  stockStatus: "Available" | "Call to confirm" | "Low stock";
};

export type CarbonProgramSnapshot = {
  activities: CarbonActivity[];
  advisories: CarbonAdvisory[];
  carbonCreditPotentialTco2e: number;
  dealers: CarbonDealer[];
  farmerParticipation: number;
  pendingActivities: number;
  profiles: CarbonProfile[];
  soilProfiles: SoilProfile[];
  totalFarmAreaAcres: number;
};

const profiles: CarbonProfile[] = [
  {
    aadhaarStatus: "Optional not captured",
    bankStatus: "Linked",
    carbonIdentityId: "CCI-MH-2026-001",
    croppingPattern: "Soybean, wheat, vegetables",
    district: "Pune",
    documents: ["Land record", "Farmer photo", "Farm geo-tag images"],
    farmerName: "Anita Patil",
    gpsLocation: "18.5204, 73.8567",
    id: "carbon-profile-1",
    irrigationSource: "Drip irrigation",
    language: "Marathi",
    livestockCount: 3,
    memberNumber: "FPO-001",
    mobileNumber: "9876543210",
    taluka: "Haveli",
    tillageStatus: "Reduced tillage",
    totalLandHoldingAcres: 4.8,
    village: "Manjari"
  },
  {
    aadhaarStatus: "Provided",
    bankStatus: "Pending",
    carbonIdentityId: "CCI-MH-2026-002",
    croppingPattern: "Pomegranate, gram",
    district: "Pune",
    documents: ["Land record", "Farmer photo"],
    farmerName: "Ramesh Jadhav",
    gpsLocation: "18.5789, 74.0021",
    id: "carbon-profile-2",
    irrigationSource: "Borewell",
    language: "Hindi",
    livestockCount: 5,
    memberNumber: "FPO-002",
    mobileNumber: "9876500123",
    taluka: "Shirur",
    tillageStatus: "Conventional",
    totalLandHoldingAcres: 6.2,
    village: "Koregaon"
  },
  {
    aadhaarStatus: "Optional not captured",
    bankStatus: "Linked",
    carbonIdentityId: "CCI-MH-2026-003",
    croppingPattern: "Turmeric, onion",
    district: "Satara",
    documents: ["Land record", "Farm geo-tag images"],
    farmerName: "Meena Shinde",
    gpsLocation: "17.6805, 74.0183",
    id: "carbon-profile-3",
    irrigationSource: "Canal",
    language: "English",
    livestockCount: 2,
    memberNumber: "FPO-003",
    mobileNumber: "9876500456",
    taluka: "Wai",
    tillageStatus: "No tillage",
    totalLandHoldingAcres: 3.7,
    village: "Menavali"
  }
];

const soilProfiles: SoilProfile[] = [
  {
    bulkDensity: 1.31,
    carbonPotentialTco2e: 8.4,
    ec: 0.42,
    id: "soil-profile-1",
    lastUpdated: "2026-05-02",
    microbialCount: "Medium",
    ndmi: 0.39,
    ndre: 0.28,
    ndvi: 0.64,
    nitrogenKgHa: 214,
    phosphorusKgHa: 18,
    ph: 6.8,
    plotName: "Plot A",
    potassiumKgHa: 278,
    recommendedInputs: ["Compost", "Mycorrhiza", "Jeevamrut"],
    soilHealthScore: 74,
    soilOrganicCarbonPercent: 0.72,
    texture: "Clay loam"
  },
  {
    bulkDensity: 1.38,
    carbonPotentialTco2e: 6.1,
    ec: 0.36,
    id: "soil-profile-2",
    lastUpdated: "2026-04-29",
    microbialCount: "Low",
    ndmi: 0.31,
    ndre: 0.22,
    ndvi: 0.51,
    nitrogenKgHa: 182,
    phosphorusKgHa: 14,
    ph: 7.2,
    plotName: "Pomegranate block",
    potassiumKgHa: 241,
    recommendedInputs: ["Vermicompost", "Trichoderma", "Mulch"],
    soilHealthScore: 62,
    soilOrganicCarbonPercent: 0.54,
    texture: "Sandy clay"
  },
  {
    bulkDensity: 1.24,
    carbonPotentialTco2e: 9.7,
    ec: 0.48,
    id: "soil-profile-3",
    lastUpdated: "2026-05-07",
    microbialCount: "High",
    ndmi: 0.44,
    ndre: 0.34,
    ndvi: 0.71,
    nitrogenKgHa: 238,
    phosphorusKgHa: 21,
    ph: 6.6,
    plotName: "Turmeric plot",
    potassiumKgHa: 296,
    recommendedInputs: ["Green manure", "Biofertilizer", "Reduced tillage"],
    soilHealthScore: 82,
    soilOrganicCarbonPercent: 0.88,
    texture: "Loam"
  }
];

const activities: CarbonActivity[] = [
  {
    activityScore: 84,
    category: "Compost Addition",
    cost: 4200,
    crop: "Soybean",
    date: "2026-05-08",
    emissionReductionTco2e: 1.2,
    evidenceCount: 3,
    id: "carbon-activity-1",
    inputUsed: "Farmyard compost",
    laborDays: 2,
    quantity: "1.5 tons",
    verificationStatus: "Verified"
  },
  {
    activityScore: 68,
    category: "Biological Application",
    cost: 1250,
    crop: "Pomegranate",
    date: "2026-05-10",
    emissionReductionTco2e: 0.6,
    evidenceCount: 2,
    id: "carbon-activity-2",
    inputUsed: "Trichoderma",
    laborDays: 1,
    quantity: "4 kg",
    verificationStatus: "Expert review"
  },
  {
    activityScore: 71,
    category: "Irrigation",
    cost: 900,
    crop: "Turmeric",
    date: "2026-05-11",
    emissionReductionTco2e: 0.4,
    evidenceCount: 1,
    id: "carbon-activity-3",
    inputUsed: "Drip irrigation",
    laborDays: 0.5,
    quantity: "45 minutes",
    verificationStatus: "AI pending"
  }
];

const advisories: CarbonAdvisory[] = [
  {
    category: "Carbon farming practice",
    channel: "In app",
    createdAt: "2026-05-09",
    id: "carbon-advisory-1",
    language: "Marathi",
    message: "Apply compost before sowing and keep crop residue on the plot.",
    status: "Published",
    title: "Residue retention for carbon build-up"
  },
  {
    category: "Biological dosage",
    channel: "Voice",
    createdAt: "2026-05-10",
    id: "carbon-advisory-2",
    language: "Hindi",
    message: "Use Trichoderma with compost for root-zone microbial support.",
    status: "Draft",
    title: "Biological input reminder"
  }
];

const dealers: CarbonDealer[] = [
  {
    category: "Biological inputs",
    distanceKm: 4.2,
    id: "dealer-1",
    name: "Green Bio Inputs",
    phone: "020-4000-1101",
    products: ["Mycorrhiza", "Trichoderma", "Bio NPK"],
    rating: 4.6,
    stockStatus: "Available"
  },
  {
    category: "Soil testing lab",
    distanceKm: 11.8,
    id: "dealer-2",
    name: "Agri Soil Lab",
    phone: "020-4000-2202",
    products: ["SOC test", "NPK panel", "pH/EC"],
    rating: 4.4,
    stockStatus: "Call to confirm"
  },
  {
    category: "Carbon farming inputs",
    distanceKm: 7.5,
    id: "dealer-3",
    name: "Regenerative Farm Supply",
    phone: "020-4000-3303",
    products: ["Mulch sheets", "Compost culture", "Cover crop seed"],
    rating: 4.2,
    stockStatus: "Low stock"
  }
];

export async function getCarbonProgramSnapshot(): Promise<CarbonProgramSnapshot> {
  return {
    activities,
    advisories,
    carbonCreditPotentialTco2e: round(
      soilProfiles.reduce((sum, profile) => sum + profile.carbonPotentialTco2e, 0)
    ),
    dealers,
    farmerParticipation: profiles.length,
    pendingActivities: activities.filter(
      (activity) => activity.verificationStatus !== "Verified"
    ).length,
    profiles,
    soilProfiles,
    totalFarmAreaAcres: round(
      profiles.reduce((sum, profile) => sum + profile.totalLandHoldingAcres, 0)
    )
  };
}

export async function getFarmerCarbonSnapshot(username: string | null) {
  const profileIndex = username ? Math.abs(hashString(username)) % profiles.length : 0;
  const profile = profiles[profileIndex];
  const soilProfile = soilProfiles[profileIndex] ?? soilProfiles[0];

  return {
    activities: activities.filter((_, index) => index === profileIndex || index === 0),
    advisories: advisories.filter((advisory) => advisory.status === "Published"),
    dealers,
    profile,
    soilProfile
  };
}

function hashString(value: string) {
  return value.split("").reduce((hash, character) => {
    return (hash << 5) - hash + character.charCodeAt(0);
  }, 0);
}

function round(value: number) {
  return Math.round(value * 100) / 100;
}
