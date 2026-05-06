import { readJsonArray, writeJson } from "../core/storage/jsonStore";
import { storageKeys } from "../core/storage/storageKeys";
import { CropCycle } from "./agricultureConfig";

export async function getSavedCropCycles(): Promise<CropCycle[]> {
  return readJsonArray<CropCycle>([
    storageKeys.activity.records,
    storageKeys.legacy.activity.records
  ]);
}

export async function saveCropCycle(cycle: CropCycle): Promise<CropCycle[]> {
  const currentCycles = await getSavedCropCycles();
  const nextCycles = [cycle, ...currentCycles];
  await writeJson(storageKeys.activity.records, nextCycles);
  return nextCycles;
}
