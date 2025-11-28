Stock management behavior implemented

What I changed:
- Block adding product to ticket when product stock <= 0.
- Block saving ticket when resulting stock after sale would be negative; transaction fails and throws a BasicException.
- After successfully saving a ticket, the UI checks each product's stock and shows a single notification (message dialog) for products that reached or fell below their `stocksecurity` (minimum) value.

Details & how to test:
1) Go to the POS sale screen and try to add a product with stock 0 (or less) — it should show a warning and not add the product.
2) If you add product(s) and try to complete the sale so that stock would become negative (e.g., selling more than available), saving will fail and the UI will show a save-ticket error with the product name.
3) When a sale reduces stock to be <= the configured minimum (stocksecurity), a dialog will appear listing the products that have hit the minimum threshold and should be restocked.

Files changed:
- `kriolos-opos-domain/src/main/java/com/openbravo/pos/forms/DataLogicSales.java`:
  - Added `findProductMinimumStock` method.
  - Added pre-save stock checks in `saveTicket` transaction to prevent negative stock.
- `kriolos-opos-app/src/main/java/com/openbravo/pos/sales/JPanelTicket.java`:
  - UI-level checks in `addTicketLine` to prevent adding product with insufficient stock.
  - Post-save low-stock check and single notification listing products below min.
- `kriolos-opos-app/src/main/resources/pos_messages*.properties`:
  - Added messages `message.stockinsufficient`, `message.stocklow`, and `message.stocklowlist`.

Notes & considerations:
- The domain-level check in `saveTicket` is required to avoid race conditions; UI checks are a convenience to prevent user errors.
- The current implementation checks the product's stock at the product level. For product bundles, the UI also checks the components after the ticket is saved and will notify about low-stock components.
- You can configure the `STOCKSECURITY` (minimum) and `STOCKMAXIMUM` (maximum) for each product and location using the existing product stock editors.

Next steps / Potential improvements:
- Provide a central low-stock management panel that aggregates low stock across locations.
- Add a configuration toggle to enable/disable strict stock enforcement (preventing sales), e.g. a POS setting to allow selling negative stock where required.
- Add unit / integration tests that simulate database entries and verify the behavior.

If you'd like, I can wire up a per-UI setting to toggle blocking sales on out-of-stock items, and add a more polished restock notification UI (e.g., allow creating a purchase order directly).
