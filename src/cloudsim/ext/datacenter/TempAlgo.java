
package cloudsim.ext.datacenter;

import cloudsim.ext.Constants;
import cloudsim.ext.event.CloudSimEvent;
import cloudsim.ext.event.CloudSimEventListener;
import cloudsim.ext.event.CloudSimEvents;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import temp_algo_st.AlgoHelper;
import temp_algo_st.MemoryComparator;
import temp_algo_st.DepConfAttr;
import temp_algo_st.DepConfList;
import temp_algo_st.Temp_Algo_Static_Var;
import temp_algo_st.AlgoAttr;
import static temp_algo_st.DepConfList.dcConfMap;

/**
 *
 * @author user
 */ 
public class TempAlgo extends VmLoadBalancer implements CloudSimEventListener {
    
    /**
     * Key : Name of the data center
     * Value : List of objects of class 'VmAllocationUIElement'.
     */
    private  Map<String,LinkedList<DepConfAttr>> confMap = new HashMap<String,LinkedList<DepConfAttr>>();
    private Iterator<Integer> availableVms = null;
    private DatacenterController dcc;
    private boolean sorted = false;
    private int currentVM;
    private boolean calledOnce = false;
    private boolean indexChanged = false;
    
    private Map<String,LinkedList<AlgoAttr>> algoMap = new HashMap<String, LinkedList<AlgoAttr>>();
    private Map<String,AlgoHelper> map = new HashMap<String,AlgoHelper>();  
    private Map<String,Integer> vmCountMap = new HashMap<String,Integer>();
    
    public TempAlgo(DatacenterController dcb) {
        confMap = DepConfList.dcConfMap;
        this.dcc = dcb;
        dcc.addCloudSimEventListener(this);
        if(!this.calledOnce) {
            this.calledOnce = true;
            // Make a new map using dcConfMap that lists 'DataCenter' as a 'key' and 'LinkedList<AlgoAttr>' as 'value'.
            Set<String> keyst =DepConfList.dcConfMap.keySet();
            for(String dataCenter : keyst) {
                LinkedList<AlgoAttr> tmpList = new LinkedList<AlgoAttr>();
                LinkedList<DepConfAttr> list = dcConfMap.get(dataCenter);
                int totalVms = 0;
                for(DepConfAttr o : list) {
                    tmpList.add(new AlgoAttr(o.getVmCount(), o.getMemory()/512, 0));
                    totalVms = totalVms + o.getVmCount();
                }
                Temp_Algo_Static_Var.algoMap.put(dataCenter, tmpList);
                Temp_Algo_Static_Var.vmCountMap.put(dataCenter, totalVms);
            }
            this.algoMap = new HashMap<String, LinkedList<AlgoAttr>>(Temp_Algo_Static_Var.algoMap);
            this.vmCountMap = new HashMap<String,Integer>(Temp_Algo_Static_Var.vmCountMap);
            this.map = new HashMap<String,AlgoHelper>(Temp_Algo_Static_Var.map);
        }
    }
    
    @Override
    public int getNextAvailableVm() {
        synchronized(this) {
           // System.out.println(Thread.currentThread().getName() + " " + this);
            //try {Thread.sleep(100000000);}catch(Exception exc){System.out.println("In the exception block");}
            System.out.println("~~~Inside the method~~~");
            String dataCenter = this.dcc.getDataCenterName();
            int totalVMs = this.vmCountMap.get(dataCenter);
            AlgoHelper ah = (AlgoHelper)this.map.get(dataCenter);
            int lastIndex = ah.getIndex();
            int lastCount = ah.getLastCount();
            LinkedList<AlgoAttr> list = this.algoMap.get(dataCenter);
            AlgoAttr aAtr = (AlgoAttr)list.get(lastIndex);
            indexChanged = false;
            if(lastCount < totalVMs)  {
                if(aAtr.getRequestAllocated() % aAtr.getWeightCount() == 0) {
                    lastCount = lastCount + 1;
                    this.currentVM = lastCount;
                    if(aAtr.getRequestAllocated() == aAtr.getVmCount() * aAtr.getWeightCount()) {
                        lastIndex++;
                        if(lastIndex != list.size()) {
                            AlgoAttr aAtr_N = (AlgoAttr)list.get(lastIndex);
                            aAtr_N.setRequestAllocated(1);
                            this.indexChanged = true;
                        }
                        if(lastIndex == list.size()) {
                            lastIndex = 0;
                            lastCount = -1;
                            this.currentVM = 0;
                            AlgoAttr aAtr_N = (AlgoAttr)list.get(lastIndex);
                            aAtr_N.setRequestAllocated(0);
                            this.indexChanged = true;

                        }
                    }
                }
                if(!this.indexChanged) {
                    aAtr.setRequestAllocated(aAtr.getRequestAllocated() + 1);
                }
                System.out.println("@@CurrVM : " + currentVM + " " + dataCenter);
                this.map.put(dataCenter, new AlgoHelper(lastIndex, lastCount));            
                return this.currentVM;
            }}

            System.out.println("--------Before final return statement---------");
            return 0;
            
    }   

    @Override
    public void cloudSimEventFired(CloudSimEvent e) {
        if(e.getId() == CloudSimEvents.EVENT_CLOUDLET_ALLOCATED_TO_VM) {
            int vmId = (Integer) e.getParameter(Constants.PARAM_VM_ID);
            //System.out.println("+++++++++++++++++++Machine with vmID : " + vmId + " attached");
        }else if(e.getId() == CloudSimEvents.EVENT_VM_FINISHED_CLOUDLET) {
            int vmId = (Integer) e.getParameter(Constants.PARAM_VM_ID);
            //System.out.println("+++++++++++++++++++Machine with vmID : " + vmId + " freed");
        }
    }
}
