//    KriolOS POS
//    Copyright (c) 2019-2023 KriolOS
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.DataRead;
import com.openbravo.data.loader.SerializerRead;
import java.io.Serializable;

/**
 * Represents a product with low stock (units <= minimum)
 * 
 * @author KriolOS
 */
public class LowStockProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    private String productId;
    private String productName;
    private String productCode;
    private String location;
    private String locationName;
    private Double units;
    private Double minimum;
    private Double maximum;
    private Double priceBuy;
    private Double priceSell;

    public LowStockProduct() {
    }

    public LowStockProduct(String productId, String productName, String productCode,
            String location, String locationName, Double units, Double minimum,
            Double maximum, Double priceBuy, Double priceSell) {
        this.productId = productId;
        this.productName = productName;
        this.productCode = productCode;
        this.location = location;
        this.locationName = locationName;
        this.units = units;
        this.minimum = minimum;
        this.maximum = maximum;
        this.priceBuy = priceBuy;
        this.priceSell = priceSell;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Double getUnits() {
        return units;
    }

    public void setUnits(Double units) {
        this.units = units;
    }

    public Double getMinimum() {
        return minimum;
    }

    public void setMinimum(Double minimum) {
        this.minimum = minimum;
    }

    public Double getMaximum() {
        return maximum;
    }

    public void setMaximum(Double maximum) {
        this.maximum = maximum;
    }

    public Double getPriceBuy() {
        return priceBuy;
    }

    public void setPriceBuy(Double priceBuy) {
        this.priceBuy = priceBuy;
    }

    public Double getPriceSell() {
        return priceSell;
    }

    public void setPriceSell(Double priceSell) {
        this.priceSell = priceSell;
    }

    public static SerializerRead getSerializerRead() {
        return new SerializerRead() {
            @Override
            public Object readValues(DataRead dr) throws BasicException {
                String productId = dr.getString(1);
                String productName = dr.getString(2);
                String productCode = dr.getString(3);
                String location = dr.getString(4);
                String locationName = dr.getString(5);
                Double units = dr.getDouble(6);
                Double minimum = dr.getDouble(7);
                Double maximum = dr.getDouble(8);
                Double priceBuy = dr.getDouble(9);
                Double priceSell = dr.getDouble(10);

                return new LowStockProduct(productId, productName, productCode,
                        location, locationName, units, minimum, maximum,
                        priceBuy, priceSell);
            }
        };
    }
}

