/** Copy into a frontend project with TypeScript and the DOM library enabled. */
export type UUID = string;
export type CustomerStatus = "ACTIVE" | "INACTIVE" | "DELETED";

export interface CreateCustomerRequest {
  username: string;
  familyName: string;
  givenName: string;
  email: string;
  phoneNumber: string;
  segmentId?: UUID | null;
}

export interface CustomerDetails extends CreateCustomerRequest {
  customerId: UUID;
  segmentId: UUID | null;
}

export interface CustomerQueryResponse extends CustomerDetails {
  status: Exclude<CustomerStatus, "DELETED">;
}

export type UpdateCustomerRequest = Pick<CreateCustomerRequest, "familyName" | "givenName">
  & Partial<Pick<CreateCustomerRequest, "username" | "email" | "phoneNumber">>;

export interface ListOptions {
  filters?: string[];
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
  direction?: "asc" | "desc";
}

export interface CustomerSegment {
  segmentId: UUID;
  name: string;
  description: string | null;
  status: "ACTIVE" | "INACTIVE";
  createdAt: string;
  updatedAt: string;
}

export interface CreateSegmentRequest {
  name: string;
  description?: string | null;
}

export interface UpdateSegmentRequest extends CreateSegmentRequest {
  status: "ACTIVE" | "INACTIVE";
}

export interface CustomerStatistics {
  total: number;
  byStatus: Array<{ status: "ACTIVE" | "INACTIVE"; total: number }>;
  bySegment: Array<{ segmentId: UUID | null; total: number }>;
  byStatusAndSegment: Array<{ status: "ACTIVE" | "INACTIVE"; segmentId: UUID | null; total: number }>;
}

function listQuery(options: ListOptions): string {
  const params = new URLSearchParams();
  for (const filter of options.filters ?? []) params.append("filter", filter);
  for (const field of ["search", "page", "size", "sort", "direction"] as const) {
    const value = options[field];
    if (value !== undefined) params.set(field, String(value));
  }
  return params.size ? `?${params.toString()}` : "";
}

// Deactivation currently returns a domain event, unlike the flat customer DTOs.
export interface CustomerDeactivatedEvent {
  customer: {
    id: { value: UUID };
    username: string;
    familyName: string;
    givenName: string;
    email: { value: string };
    phoneNumber: { value: string };
    status: "INACTIVE";
  };
  initiatedAt: string;
}

export interface CreateBusinessRequest {
  name: string;
  email: string;
  address: string;
}

export interface BusinessResponse extends CreateBusinessRequest {
  businessId: UUID;
  status: "ACTIVE";
  createdAt: string;
}

export interface CreateOrderRequest {
  customerId: UUID;
  businessId: UUID;
  deliveryAddress: string;
  items: Array<{ productId: UUID; quantity: number; unitPrice: number }>;
}

export interface OrderResponse {
  orderId: UUID;
  customerId: UUID;
  businessId: UUID;
  total: number;
  status: "PENDING";
  createdAt: string;
}

export interface CreatePaymentRequest {
  orderId: UUID;
  customerId: UUID;
  amount: number;
}

export interface PaymentResponse extends CreatePaymentRequest {
  paymentId: UUID;
  status: "PENDING";
  createdAt: string;
}

export interface ApiBaseUrls {
  customer: string;
  business: string;
  order: string;
  payment: string;
}

export class ApiError extends Error {
  constructor(public readonly status: number, message: string, public readonly body: unknown) {
    super(message);
    this.name = "ApiError";
  }
}

function errorMessage(status: number, body: unknown): string {
  if (body !== null && typeof body === "object") {
    const fields = body as Record<string, unknown>;
    for (const field of ["error", "detail", "message"]) {
      if (typeof fields[field] === "string" && fields[field]) return fields[field] as string;
    }
  }
  const fallback: Record<number, string> = {
    400: "Check the submitted fields and customer ID.",
    404: "This resource was not found or was deleted.",
    409: "The requested action conflicts with the customer's current state.",
  };
  return fallback[status] ?? `Request failed (HTTP ${status}).`;
}

async function request<T>(url: string, method: string, body?: unknown, signal?: AbortSignal): Promise<T> {
  const response = await fetch(url, {
    method,
    headers: body === undefined
      ? { Accept: "application/json" }
      : { Accept: "application/json", "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  });

  // Soft deletion returns 204; tolerate empty responses from any service.
  const text = response.status === 204 ? "" : await response.text();
  let data: unknown = undefined;
  if (text) {
    try { data = JSON.parse(text); }
    catch {
      if (response.ok) throw new Error("The API returned an unexpected non-JSON response.");
      data = text;
    }
  }
  if (!response.ok) throw new ApiError(response.status, errorMessage(response.status, data), data);
  return data as T;
}

/** Defaults require the same-origin proxy routes documented in FRONTEND_INTEGRATION.md. */
export function createFrontendApi(base: ApiBaseUrls = {
  customer: "/backend/customer",
  business: "/backend/business",
  order: "/backend/order",
  payment: "/backend/payment",
}) {
  const url = (service: keyof ApiBaseUrls, path: string) => base[service].replace(/\/$/, "") + path;
  const customerPath = (id: UUID) => `/api/customers/${encodeURIComponent(id)}`;

  return {
    customers: {
      list: (options: ListOptions = {}, signal?: AbortSignal) =>
        request<CustomerQueryResponse[]>(url("customer", "/api/customers" + listQuery(options)), "GET", undefined, signal),
      get: (id: UUID, signal?: AbortSignal) =>
        request<CustomerQueryResponse>(url("customer", customerPath(id)), "GET", undefined, signal),
      statistics: (options: Pick<ListOptions, "filters" | "search"> = {}, signal?: AbortSignal) =>
        request<CustomerStatistics>(url("customer", "/api/customers/statistics" + listQuery(options)), "GET", undefined, signal),
      setStatus: (id: UUID, status: "ACTIVE" | "INACTIVE") =>
        request<CustomerQueryResponse>(url("customer", customerPath(id) + "/status"), "PATCH", { status }),
      assignSegment: (id: UUID, segmentId: UUID) =>
        request<CustomerQueryResponse>(url("customer", customerPath(id) + "/segment"), "PUT", { segmentId }),
      clearSegment: (id: UUID) =>
        request<CustomerQueryResponse>(url("customer", customerPath(id) + "/segment"), "DELETE"),
      create: (body: CreateCustomerRequest) =>
        request<CustomerDetails>(url("customer", "/api/customers"), "POST", body),
      update: (id: UUID, body: UpdateCustomerRequest) =>
        request<Omit<CustomerDetails, "segmentId">>(url("customer", customerPath(id)), "PUT", body),
      deactivate: (id: UUID) =>
        request<CustomerDeactivatedEvent>(url("customer", customerPath(id) + "/deactivate"), "PUT"),
      remove: (id: UUID) =>
        request<void>(url("customer", customerPath(id)), "DELETE"),
    },
    segments: {
      create: (body: CreateSegmentRequest) =>
        request<CustomerSegment>(url("customer", "/api/customer-segments"), "POST", body),
      list: (options: ListOptions = {}, signal?: AbortSignal) =>
        request<CustomerSegment[]>(url("customer", "/api/customer-segments" + listQuery(options)), "GET", undefined, signal),
      get: (id: UUID) =>
        request<CustomerSegment>(url("customer", `/api/customer-segments/${encodeURIComponent(id)}`), "GET"),
      update: (id: UUID, body: UpdateSegmentRequest) =>
        request<CustomerSegment>(url("customer", `/api/customer-segments/${encodeURIComponent(id)}`), "PUT", body),
      remove: (id: UUID) =>
        request<void>(url("customer", `/api/customer-segments/${encodeURIComponent(id)}`), "DELETE"),
    },
    businesses: {
      create: (body: CreateBusinessRequest) =>
        request<BusinessResponse>(url("business", "/api/businesses"), "POST", body),
    },
    orders: {
      create: (body: CreateOrderRequest) =>
        request<OrderResponse>(url("order", "/api/orders"), "POST", body),
    },
    payments: {
      create: (body: CreatePaymentRequest) =>
        request<PaymentResponse>(url("payment", "/api/payments"), "POST", body),
    },
  };
}
