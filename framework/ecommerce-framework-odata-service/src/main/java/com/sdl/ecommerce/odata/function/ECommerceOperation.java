package com.sdl.ecommerce.odata.function;

import com.sdl.ecommerce.odata.model.ODataQueryResult;
import com.sdl.ecommerce.odata.service.ODataRequestContextHolder;
import com.sdl.odata.api.ODataException;
import com.sdl.odata.api.edm.model.Operation;
import com.sdl.odata.api.processor.datasource.factory.DataSourceFactory;
import com.sdl.odata.api.service.ODataRequestContext;

/**
 * ECommerce Operation
 *
 * @author nic
 */
public abstract class ECommerceOperation implements Operation<ODataQueryResult> {

    // TODO: Refactor this to not be dependent to ODataQueryResult

    @Override
    public ODataQueryResult doOperation(ODataRequestContext oDataRequestContext, DataSourceFactory dataSourceFactory) throws ODataException {

        ODataRequestContextHolder.set(oDataRequestContext);
        try {
            return this.doECommerceOperation(oDataRequestContext, dataSourceFactory);
        }
        finally {
            ODataRequestContextHolder.clear();
        }
    }

    protected abstract ODataQueryResult doECommerceOperation(ODataRequestContext oDataRequestContext, DataSourceFactory dataSourceFactory) throws ODataException;
}
