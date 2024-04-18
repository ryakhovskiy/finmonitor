package yahoofinance.histquotes2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by Stijn on 23/05/2017.
 */
public class CrumbManager {

    private static final Logger log = LogManager.getLogger(CrumbManager.class);

    private static final org.kr.stocksmonitor.yahoo.CrumbManager crumbManager =
            org.kr.stocksmonitor.yahoo.CrumbManager.getInstance();
}