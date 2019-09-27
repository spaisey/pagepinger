package uk.co.itello.pinger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ManagementController {
    private static final Logger LOG = LoggerFactory.getLogger(ManagementController.class);
    private PageAgent pageAgent;

    public ManagementController(PageAgent pageAgent) {
        this.pageAgent = pageAgent;
    }

    @RequestMapping("reset")
    public String reset() {
        LOG.info("Reset request received");
        pageAgent.reset();
        return "Reset complete";
    }
}
