export interface PollUntilOptions {
  intervalMs?: number;
  maxAttempts?: number;
}

export async function pollUntil<T>(
  load: () => Promise<T>,
  predicate: (value: T) => boolean,
  { intervalMs = 900, maxAttempts = 7 }: PollUntilOptions = {},
): Promise<{ value: T; matched: boolean }> {
  let value = await load();
  if (predicate(value)) {
    return { value, matched: true };
  }

  for (let attempt = 1; attempt < maxAttempts; attempt += 1) {
    await wait(intervalMs);
    value = await load();
    if (predicate(value)) {
      return { value, matched: true };
    }
  }

  return { value, matched: false };
}

function wait(ms: number): Promise<void> {
  return new Promise((resolve) => {
    window.setTimeout(resolve, ms);
  });
}
