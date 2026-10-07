import type { ApiProblem } from "./types";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
  }
}

export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const isFormData = typeof FormData !== "undefined" && init?.body instanceof FormData;
  const response = await fetch(path, {
    ...init,
    cache: "no-store",
    headers: {
      ...(init?.body && !isFormData ? { "Content-Type": "application/json" } : {}),
      ...init?.headers,
    },
  });

  if (!response.ok) {
    let problem: ApiProblem = {};
    try {
      problem = (await response.json()) as ApiProblem;
    } catch {
      // A proxy/network failure may not return JSON.
    }
    throw new ApiError(
      response.status,
      problem.code ?? "REQUEST_FAILED",
      problem.detail ?? problem.title ?? "Request failed",
    );
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}
