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

async function wait(ms: number): Promise<void> {
  await new Promise<void>((resolve) => {
    window.setTimeout(resolve, ms);
  });
  await waitUntilVisible();
}

function waitUntilVisible(): Promise<void> {
  if (typeof document === "undefined" || document.visibilityState !== "hidden") {
    return Promise.resolve();
  }

  return new Promise((resolve) => {
    document.addEventListener(
      "visibilitychange",
      function onVisible() {
        if (document.visibilityState !== "hidden") {
          document.removeEventListener("visibilitychange", onVisible);
          resolve();
        }
      },
    );
  });
}
