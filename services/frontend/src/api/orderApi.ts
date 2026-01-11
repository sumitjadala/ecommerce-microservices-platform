import axiosClient from './axiosClient';

export interface Product {
  id: string;
  name: string;
  price: number;
  image: string;
  description: string;
}

export interface Order {
  id: number;
  orderId?: string; // alias for id
  productId?: string;
  productName?: string;
  amount: number;
  status: 'CREATED' | 'PAYMENT_PENDING' | 'PAID' | 'PAYMENT_FAILED' | 'CANCELLED';
  paymentStatus: 'PENDING' | 'COMPLETED' | 'PAID' | 'FAILED' | 'REFUNDED' | 'CANCELLED';
  createdAt: string;
  productIds?: number[];
}

export interface CreateOrderRequest {
  productId: string;
  productName: string;
  amount: number;
}

// Use direct fetch to backend for order status (bypassing mock axiosClient)
const ORDER_API = 'http://localhost:8081/api/v1/orders';

export const createOrder = async (data: CreateOrderRequest): Promise<Order> => {
  const response = await axiosClient.post<Order>('/api/orders', data);
  return response.data;
};

export const getOrderStatus = async (orderId: string): Promise<Order> => {
  const response = await fetch(`${ORDER_API}/${orderId}`);
  if (!response.ok) {
    throw new Error(`Failed to fetch order status: ${response.status}`);
  }
  return response.json();
};
