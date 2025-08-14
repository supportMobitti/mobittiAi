package org.mobitti.controllers;

import io.micrometer.common.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mobitti.system.RestException;
import org.mobitti.system.Status;
import org.mobitti.system.UnknownRestException;
import org.springframework.web.util.NestedServletException;

public class BaseController {

    private static final Logger logger = LogManager.getLogger(AiController.class);

    public static Status handleException(Exception ex) {
        return handleException(ex, "he");
    }

    public static Status handleException (Exception ex,String lang)
    {
        ex.printStackTrace();
        Status status=null;

        if (ex instanceof RestException)
        {

            status = ((RestException) ex).getStatusDoc();
            logger.error(((RestException) ex).getStatusDoc().getDeveloperErrorMessage(),ex);
        }
        else if (ex instanceof NestedServletException)
        {
            Throwable e = ex.getCause();
            if (e instanceof RestException)
            {
                status = ((RestException) e).getStatusDoc();
                logger.error(((RestException) e).getStatusDoc().getDeveloperErrorMessage(),e);
            }
        }

        if (status == null)	{
            logger.error(ex, ex);
            UnknownRestException e ;
            try
            {
                String err="";
                try {
                    err = "Internal server error";
                }
                catch (Exception e3) {
                    e3.printStackTrace();
                }
                if (!StringUtils.isEmpty(err)) {
                    e = new UnknownRestException(err);
                }
                else {
                    e = new UnknownRestException("genericServerError");
                }
            }
            catch (Exception exc) {
                e = new UnknownRestException("server error");
            }
            status = e.getStatusDoc();
        }
        return status;
    }
}
