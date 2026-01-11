import { useState, useEffect } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import Layout from '@/components/layout/Layout';
import { Button } from '@/components/ui/button';
import StatusBadge from '@/components/order/StatusBadge';
import { useCart, CartItem } from '@/hooks/useCart';
import { createBackendOrder, initiatePaymentForOrder } from '@/api/razorpayApi';
import { loadRazorpayScript } from '@/lib/razorpay';

interface LocationState {
  productName?: string;
  amount?: number;
  cartItems?: CartItem[];
}

const Checkout = () => {
  const { orderId } = useParams<{ orderId: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const { items: currentCartItems, totalAmount: currentTotal, clearCart } = useCart();
  const state = location.state as LocationState | null;
  
  const [isProcessing, setIsProcessing] = useState(false);
  const [processingMessage, setProcessingMessage] = useState<string | null>(null);
  const [serverOrderId, setServerOrderId] = useState<string | null>(null);
  const [paymentError, setPaymentError] = useState<string | null>(null);
  const displayOrderId = String(serverOrderId || orderId || 'pending-order');
  const isPreview = Boolean(orderId && orderId.startsWith('preview-'));
  const currentStatus = isProcessing ? 'PAYMENT_IN_PROGRESS' : isPreview ? 'INITIATED' : 'CREATED';

  // Use passed state or current cart
  const cartItems = state?.cartItems || currentCartItems;
  const amount = state?.amount || currentTotal;
  const productName = state?.productName || cartItems.map((i) => i.name).join(', ') || 'Demo Product';

  // Redirect if no items
  useEffect(() => {
    if (cartItems.length === 0 && !state?.amount) {
      navigate('/');
    }
  }, [cartItems.length, state?.amount, navigate]);

  const toProductIdNumber = (id: string): number => {
    const numeric = parseInt(id.replace(/\D/g, ''), 10);
    return Number.isFinite(numeric) && numeric > 0 ? numeric : 0;
  };

  const handlePayment = async () => {
    if (!amount || cartItems.length === 0) return;

    const productIds = cartItems
      .map((item) => toProductIdNumber(item.id))
      .filter((id) => id > 0);

    if (productIds.length === 0) {
      setPaymentError('Unable to map product IDs to numeric values.');
      return;
    }

    setIsProcessing(true);
    setPaymentError(null);
    setProcessingMessage('Preparing payment...');

    try {
      // STEP 1: Create order (if not already created)
      let backendOrderId = serverOrderId || orderId;

      if (!backendOrderId) {
        setProcessingMessage('Creating order...');
        const order = await createBackendOrder({ userId: 1, productIds, amount });
        backendOrderId = order.orderId || order.id;
        setServerOrderId(backendOrderId);
      }

      // STEP 3: User clicked "Pay Now" - Initiate payment (calls Razorpay)
      setProcessingMessage('Initiating payment...');
      const paymentDetails = await initiatePaymentForOrder(backendOrderId as string);

      await loadRazorpayScript();

      const rzp = new window.Razorpay({
        key: paymentDetails.keyId,
        amount: paymentDetails.amount,
        currency: paymentDetails.currency || 'INR',
        name: 'Atelier',
        description: productName,
        order_id: paymentDetails.razorpayOrderId,
        handler: () => {
          setProcessingMessage('Payment successful! Redirecting...');
          clearCart();
          // Use replace + setTimeout to ensure navigation happens after Razorpay modal closes
          setTimeout(() => {
            navigate(`/order/${backendOrderId}`, {
              state: { productName, amount, cartItems },
              replace: true,
            });
          }, 100);
        },
        modal: {
          ondismiss: () => {
            setIsProcessing(false);
            setProcessingMessage(null);
          },
        },
        prefill: {
          name: 'Demo User',
          email: 'demo@example.com',
        },
        theme: {
          color: '#111827',
        },
      });

      rzp.on('payment.failed', () => {
        setIsProcessing(false);
        setProcessingMessage(null);
        setPaymentError('Payment failed or cancelled. Please try again.');
      });

      rzp.open();
    } catch (error: any) {
      console.error('Payment initiation failed', error);
      setPaymentError(error?.message || 'Something went wrong while starting the payment.');
      setIsProcessing(false);
      setProcessingMessage(null);
    }
  };

  if (cartItems.length === 0 && !state?.amount) {
    return null;
  }

  return (
    <Layout>
      <div className="mx-auto max-w-lg animate-fade-in">
        <header className="mb-8 text-center">
          <h1 className="font-serif text-3xl md:text-4xl">Checkout</h1>
          <p className="mt-2 text-muted-foreground font-light">
            Review and complete your order
          </p>
        </header>

        <div className="space-y-6">
          {/* Order Summary Card */}
          <div className="rounded border border-border bg-card p-6">
            <div className="flex items-center justify-between border-b border-border pb-4">
              <span className="text-sm text-muted-foreground font-light">Order ID</span>
              <code className="text-sm font-mono bg-secondary px-2 py-1 rounded">
                  {displayOrderId.slice(0, 12)}...
              </code>
            </div>
            
            <div className="py-4 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">Status</span>
                <StatusBadge status={currentStatus} />
              </div>
              
              {/* Cart Items */}
              <div className="border-t border-border pt-4 space-y-3">
                <span className="text-sm text-muted-foreground">Items</span>
                {cartItems.map((item) => (
                  <div key={item.id} className="flex items-center gap-3">
                    <div className="h-12 w-10 flex-shrink-0 overflow-hidden rounded bg-secondary">
                      <img
                        src={item.image}
                        alt={item.name}
                        className="h-full w-full object-cover"
                      />
                    </div>
                    <div className="flex-1">
                      <p className="text-sm font-serif">{item.name}</p>
                      <p className="text-xs text-muted-foreground">
                        Qty: {item.quantity} × ₹{item.price}
                      </p>
                    </div>
                    <span className="text-sm tabular-nums">
                      ₹{item.price * item.quantity}
                    </span>
                  </div>
                ))}
              </div>
              
              <div className="flex items-center justify-between pt-4 border-t border-border">
                <span className="text-muted-foreground">Total</span>
                <span className="font-serif text-2xl">₹{amount}</span>
              </div>
            </div>
          </div>

          {/* Payment Section */}
          <div className="rounded border border-border bg-card p-6">
            <h2 className="font-serif text-lg mb-4">Payment Method</h2>
            <p className="text-sm text-muted-foreground mb-6 font-light">
              Payment will be processed securely by our backend payment service via Razorpay.
            </p>
            
            <Button
              onClick={handlePayment}
              disabled={isProcessing}
              className="w-full"
              size="lg"
            >
              {isProcessing ? (
                <span className="flex items-center gap-2">
                  <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  {processingMessage || 'Initiating payment...'}
                </span>
              ) : (
                `Pay ₹${amount}`
              )}
            </Button>

            {isPreview && !isProcessing && (
              <p className="mt-3 text-sm text-muted-foreground text-center">
                You just clicked checkout — click <strong>Pay</strong> to create the order and continue to payment.
              </p>
            )}

            {processingMessage && (
              <p className="mt-3 text-sm text-muted-foreground text-center">{processingMessage}</p>
            )}

            {paymentError && (
              <p className="mt-3 text-sm text-destructive text-center">{paymentError}</p>
            )}
          </div>

          {/* Demo Notice */}
          <aside className="text-center text-sm text-muted-foreground">
            <p className="font-light">
              After checkout, we wait for the backend to confirm the payment via webhook before marking it paid.
            </p>
          </aside>
        </div>
      </div>
    </Layout>
  );
};

export default Checkout;
