import { readJsonArray, writeJson } from "../core/storage/jsonStore";
import { storageKeys } from "../core/storage/storageKeys";
import { ProofSubmission } from "./farmDemoData";

export async function getSavedProofs(): Promise<ProofSubmission[]> {
  return readJsonArray<ProofSubmission>([
    storageKeys.evidence.records,
    storageKeys.legacy.evidence.records
  ]);
}

export async function saveProof(
  submission: ProofSubmission
): Promise<ProofSubmission[]> {
  const currentProofs = await getSavedProofs();
  const nextProofs = [
    submission,
    ...currentProofs.filter(
      (proof) =>
        proof.cycleId !== submission.cycleId || proof.stepId !== submission.stepId
    )
  ];
  await writeJson(storageKeys.evidence.records, nextProofs);
  return nextProofs;
}
