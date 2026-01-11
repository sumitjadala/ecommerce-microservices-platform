import { useState, useEffect, useCallback } from 'react';
import { useParams, useLocation, Link } from 'react-router-dom';
import Layout from '@/components/layout/Layout';
import StatusBadge from '@/components/order/StatusBadge';
import { Button } from '@/components/ui/button';
import { getOrderStatus, Order } from '@/api/orderApi';

type OrderStatusType = 'CREATED' | 'PENDING' | 'PAID' | 'FAILED' | 'COMPLETED';

interface LocationState {
  productName?: string;
  amount?: number;
}

// Status flow for display
const STATUS_FLOW: OrderStatusType[] = [
  'CREATED',
  'PENDING',
  'PAID',
];

const OrderStatus = () => {
  const { orderId } = useParams<{ orderId: string }>();
  const location = useLocation();
  const state = location.state as LocationState | null;

  const [order, setOrder] = useState<Order | null>(null);
  const [isPolling, setIsPolling] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Use passed state or order data
  const productName = state?.productName || 'Demo Product';
  const amount = state?.amount || order?.amount || 0;

  const fetchStatus = useCallback(async () => {
    if (!orderId) return;

    try {
      const orderData = await getOrderStatus(orderId);
      setOrder(orderData);
      setError(null);
      
      // Stop polling when payment reaches final state
      const paymentStatus = orderData.paymentStatus;
      if (paymentStatus === 'COMPLETED' || paymentStatus === 'PAID' || paymentStatus === 'FAILED') {
        setIsPolling(false);
      }
    } catch (err) {
      console.error('Error fetching order status:', err);
      setError('Unable to fetch order status');
    }
  }, [orderId]);

  // Polling effect
  useEffect(() => {
    if (!isPolling) return;

    fetchStatus();
    const interval = setInterval(fetchStatus, 2000);

    return () => clearInterval(interval);
  }, [fetchStatus, isPolling]);

  // Map payment status to display status
  const getDisplayStatus = (): OrderStatusType => {
    if (!order) return 'PENDING';
    
    const paymentStatus = order.paymentStatus;
    if (paymentStatus === 'COMPLETED' || paymentStatus === 'PAID') return 'PAID';
    if (paymentStatus === 'FAILED') return 'FAILED';
    if (paymentStatus === 'PENDING') return 'PENDING';
    return 'CREATED';
  };

  const currentStatus = getDisplayStatus();
  const isFinalState = currentStatus === 'PAID' || currentStatus === 'FAILED';

  return (
    <Layout>
      <div className="mx-auto max-w-lg animate-fade-in">
        <header className="mb-8 text-center">
          <h1 className="font-serif text-3xl md:text-4xl">Order Status</h1>
          <p className="mt-2 text-muted-foreground font-light">
            {isPolling ? 'Monitoring your order...' : 'Order complete'}
          </p>
        </header>

        <div className="space-y-6">
          {/* Status Card */}
          <div className="rounded border border-border bg-card p-6">
            <div className="flex items-center justify-between border-b border-border pb-4">
              <span className="text-sm text-muted-foreground font-light">Order ID</span>
              <code className="text-sm font-mono bg-secondary px-2 py-1 rounded">
                {orderId?.slice(0, 12)}...
              </code>
            </div>

            <div className="py-6 space-y-6">
              {/* Current Status */}
              <div className="text-center">
                <StatusBadge status={currentStatus} />
              </div>

              {/* Status Timeline */}
              <div className="space-y-4">
                {STATUS_FLOW.map((status, index) => {
                  const isActive = STATUS_FLOW.indexOf(currentStatus as any) >= index;
                  const isCurrent = currentStatus === status;
                  
                  return (
                    <div
                      key={status}
                      className={`flex items-center gap-4 transition-opacity duration-300 ${
                        isActive ? 'opacity-100' : 'opacity-30'
                      }`}
                    >
                      <div
                        className={`h-3 w-3 rounded-full transition-colors ${
                          isActive ? 'bg-foreground' : 'bg-border'
                        } ${isCurrent && isPolling ? 'animate-pulse' : ''}`}
                      />
                      <span className={`text-sm ${isActive ? 'text-foreground' : 'text-muted-foreground'}`}>
                        {status === 'CREATED' ? 'Order Created' : 
                         status === 'PENDING' ? 'Payment Processing' : 
                         status === 'PAID' ? 'Payment Completed' : status.replace(/_/g, ' ')}
                      </span>
                      {isCurrent && isPolling && (
                        <span className="text-xs text-muted-foreground">
                          Processing...
                        </span>
                      )}
                    </div>
                  );
                })}
              </div>

              {/* Order Details */}
              <div className="pt-4 border-t border-border space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Product</span>
                  <span>{productName}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">Amount</span>
                  <span className="font-serif text-lg">₹{amount}</span>
                </div>
                {order && (
                  <>
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Order Status</span>
                      <span>{order.status}</span>
                    </div>
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Payment Status</span>
                      <span>{order.paymentStatus}</span>
                    </div>
                  </>
                )}
              </div>
            </div>
          </div>

          {/* Success Message */}
          {currentStatus === 'PAID' && (
            <div className="rounded border border-success/20 bg-success/5 p-4 text-center">
              <svg className="mx-auto h-8 w-8 text-success mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
              <p className="text-sm font-medium text-success">Payment successful!</p>
              <p className="text-xs text-muted-foreground mt-1">Thank you for your order.</p>
            </div>
          )}

          {/* Failed Message */}
          {currentStatus === 'FAILED' && (
            <div className="rounded border border-destructive/20 bg-destructive/5 p-4 text-center">
              <svg className="mx-auto h-8 w-8 text-destructive mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
              <p className="text-sm font-medium text-destructive">Payment failed</p>
              <p className="text-xs text-muted-foreground mt-1">Please try again.</p>
            </div>
          )}
          
          {/* Error Message */}
          {error && (
            <div className="rounded border border-yellow-500/20 bg-yellow-500/5 p-4 text-center">
              <p className="text-sm text-yellow-600">{error}</p>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-4">
            <Button asChild variant="outline" className="flex-1">
              <Link to="/">Continue Shopping</Link>
            </Button>
            {isFinalState && (
              <Button asChild className="flex-1">
                <Link to="/">New Order</Link>
              </Button>
            )}
          </div>

          {/* Status Notice */}
          <aside className="text-center text-sm text-muted-foreground">
            <p className="font-light">
              {isPolling 
                ? 'Waiting for payment confirmation from Razorpay webhook...'
                : 'Order processing complete.'
              }
            </p>
          </aside>
        </div>
      </div>
    </Layout>
  );
};

export default OrderStatus;
