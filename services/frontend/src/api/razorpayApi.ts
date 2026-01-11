export interface BackendOrderRequest {
  userId: number;
  productIds: number[];
  amount: number;
}

export interface BackendOrderResponse {
  orderId: string;
  [key: string]: any;
}

export interface RazorpayPaymentDetails {
  razorpayOrderId: string;
  amount: number;
  keyId: string;
  currency?: string;
}

const ORDER_API = 'http://localhost:8081/api/v1/orders';
const PAYMENT_API = 'http://localhost:8082/api/v1/payments';

export const createBackendOrder = async (payload: BackendOrderRequest): Promise<BackendOrderResponse> => {
  const response = await fetch(ORDER_API, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`Order creation failed (${response.status}): ${text || response.statusText}`);
  }

  return response.json();
};

/**
 * STEP 3: User clicks "Pay Now" - Initiate payment for an existing order.
 * This triggers Razorpay order creation in Payment Service.
 * Returns the Razorpay order details needed for checkout.
 */
export const initiatePaymentForOrder = async (
  orderId: string,
): Promise<RazorpayPaymentDetails> => {
  const response = await fetch(`${PAYMENT_API}/order/${orderId}/initiate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`Payment initiation failed (${response.status}): ${text || response.statusText}`);
  }

  return response.json();
};

export const fetchRazorpayPaymentDetails = async (
  orderId: string,
): Promise<RazorpayPaymentDetails | null> => {
  const response = await fetch(`${PAYMENT_API}/order/${orderId}/razorpay`);

  if (response.status === 404 || response.status === 202) {
    return null;
  }

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`Unable to fetch payment details (${response.status}): ${text || response.statusText}`);
  }

  return response.json();
};

/**
 * Poll for Razorpay payment details (legacy - used when waiting for async creation).
 * For the new flow, use initiatePaymentForOrder() which is synchronous.
 */
export const pollRazorpayPaymentDetails = async (
  orderId: string,
  attempts = 10,
  delayMs = 500,
): Promise<RazorpayPaymentDetails> => {
  for (let i = 0; i < attempts; i++) {
    const details = await fetchRazorpayPaymentDetails(orderId);
    if (details?.razorpayOrderId && details.keyId) {
      return details;
    }
    await new Promise((resolve) => setTimeout(resolve, delayMs));
  }

  throw new Error('Timed out waiting for Razorpay payment details');
};
