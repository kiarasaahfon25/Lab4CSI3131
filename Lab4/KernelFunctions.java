public class KernelFunctions
{
    //******************************************************************
    //                 Methods for supporting page replacement
    //******************************************************************

    // the following method calls a page replacement method once
    // all allocated frames have been used up
    // DO NOT CHANGE this method
    public static void pageReplacement(int vpage, Process prc, Kernel krn)
    {
        if(prc.pageTable[vpage].valid) return;   // no need to replace

        if(!prc.areAllocatedFramesFull()) // room to get frames in allocated list
            addPageFrame(vpage, prc, krn);
        else
            pageReplAlgorithm(vpage, prc, krn);
    }

    // This method will all a page frame to the list of allocated 
    // frames for the process and load it with the virtual page
    // DO NOT CHANGE this method
    public static void addPageFrame(int vpage, Process prc, Kernel krn)
    {
        int [] fl;  // new frame list
        int freeFrame;  // a frame from the free list
        int i;
        // Get a free frame and update the allocated frame list
        freeFrame = krn.getNextFreeFrame();  // gets next free frame
        if(freeFrame == -1)  // list must be empty - print error message and return
        {
            System.out.println("Could not get a free frame");
            return;
        }
        if(prc.allocatedFrames == null) // No frames allocated yet
        {
            prc.allocatedFrames = new int[1];
            prc.allocatedFrames[0] = freeFrame;
        }
        else // some allocated but can get more
        {
            fl = new int[prc.allocatedFrames.length+1];
            for(i=0 ; i<prc.allocatedFrames.length ; i++) fl[i] = prc.allocatedFrames[i];
            fl[i] = freeFrame; // adds free frame to the free list
            prc.allocatedFrames = fl; // keep new list
        }
        // update Page Table
        prc.pageTable[vpage].frameNum = freeFrame;
        prc.pageTable[vpage].valid = true;
    }

    // Calls to Replacement algorithm
    public static void pageReplAlgorithm(int vpage, Process prc, Kernel krn)
    {
        boolean doingCount = false;
        switch(krn.pagingAlgorithm)
        {
            case FIFO: pageReplAlgorithmFIFO(vpage, prc); break;
            case LRU: pageReplAlgorithmLRU(vpage, prc); break;
            case CLOCK: pageReplAlgorithmCLOCK(vpage, prc); break;
            case COUNT: pageReplAlgorithmCOUNT(vpage, prc); doingCount=true; break;
        }
    }
    
    //--------------------------------------------------------------
    //  The following methods need modification to implement
    //  the three page replacement algorithms
    //  ------------------------------------------------------------
    // The following method is called each time an access to memory
    // is made (including after a page fault). It will allow you
    // to update the page table entries for supporting various
    // page replacement algorithms.
    public static void doneMemAccess(int vpage, Process prc, double clock)
    {
        // Update usage for CLOCK
        prc.pageTable[vpage].used = true;

        // Update time stamp for LRU
        prc.pageTable[vpage].tmStamp = clock;  // clock can be System.currentTimeMillis() or a simulation time

        // Update count for COUNT
        prc.pageTable[vpage].count++;
    }


    // FIFO page Replacement algorithm
    public static void pageReplAlgorithmFIFO(int vpage, Process prc)
    {
        int vPageReplaced;  // Page to be replaced
        int frame;  // frame to receive new page
        // Find page to be replaced
        frame = prc.allocatedFrames[prc.framePtr];   // get next available frame
        vPageReplaced = findvPage(prc.pageTable,frame);     // find current page using it (i.e written to disk)
        prc.pageTable[vPageReplaced].valid = false;  // Old page is replaced.
        prc.pageTable[vpage].frameNum = frame;   // load page into the frame and update table
        prc.pageTable[vpage].valid = true;             // make the page valid
        prc.framePtr = (prc.framePtr+1) % prc.allocatedFrames.length;  // point to next frame in list
    }

    // CLOCK page Replacement algorithm
    public static void pageReplAlgorithmCLOCK(int vpage, Process prc)
    {
        int framesCount = prc.allocatedFrames.length;
        while (true)
        {
            int frame = prc.allocatedFrames[prc.framePtr];
            int vPageReplaced = findvPage(prc.pageTable, frame);
            if (vPageReplaced == -1) {
                System.out.println("Error: frame not found in page table.");
                return;
            }
            if (!prc.pageTable[vPageReplaced].used)
            {  // Replace this page
                prc.pageTable[vPageReplaced].valid = false;
                prc.pageTable[vpage].frameNum = frame;
                prc.pageTable[vpage].valid = true;
                prc.pageTable[vpage].used = true; // new page accessed
                prc.framePtr = (prc.framePtr + 1) % framesCount;
                break;
            }
            else
            {
                // Give second chance, clear used bit
                prc.pageTable[vPageReplaced].used = false;
                prc.framePtr = (prc.framePtr + 1) % framesCount;
            }
        }
    }

    public static void pageReplAlgorithmLRU(int vpage, Process prc)
    {
        int leastRecentPage = -1;
        double oldestTimestamp = Double.MAX_VALUE;

        // Find the valid page with the oldest timestamp
        for (int i = 0; i < prc.pageTable.length; i++)
        {
            if (prc.pageTable[i].valid)
            {
                if (prc.pageTable[i].tmStamp < oldestTimestamp)
                {
                    oldestTimestamp = prc.pageTable[i].tmStamp;
                    leastRecentPage = i;
                }
            }
        }

        if (leastRecentPage == -1)
        {
            System.out.println("LRU: No valid pages found for replacement.");
            return;
        }

        // Replace least recently used page
        int frame = prc.pageTable[leastRecentPage].frameNum;
        prc.pageTable[leastRecentPage].valid = false;
        prc.pageTable[vpage].frameNum = frame;
        prc.pageTable[vpage].valid = true;
        prc.pageTable[vpage].tmStamp = System.currentTimeMillis(); // or use passed clock in doneMemAccess
    }


    // COUNT page Replacement algorithm
    public static void pageReplAlgorithmCOUNT(int vpage, Process prc)
    {
        int leastCountPage = -1;
        long smallestCount = Long.MAX_VALUE;

        // Find page with smallest count
        for (int i = 0; i < prc.pageTable.length; i++)
        {
            if (prc.pageTable[i].valid)
            {
                if (prc.pageTable[i].count < smallestCount)
                {
                    smallestCount = prc.pageTable[i].count;
                    leastCountPage = i;
                }
            }
        }

        if (leastCountPage == -1)
        {
            System.out.println("COUNT: No valid pages found for replacement.");
            return;
        }

        // Replace the page with smallest count
        int frame = prc.pageTable[leastCountPage].frameNum;
        prc.pageTable[leastCountPage].valid = false;
        prc.pageTable[vpage].frameNum = frame;
        prc.pageTable[vpage].valid = true;
        prc.pageTable[vpage].count = 0; // reset count for new page
    }


    // finds the virtual page loaded in the specified frame fr
    public static int findvPage(PgTblEntry [] ptbl, int fr)
    {
        int i;
        for(i=0 ; i<ptbl.length ; i++)
        {
            if(ptbl[i].valid)
            {
                if(ptbl[i].frameNum == fr)
                {
                    return(i);
                }
            }
        }
        System.out.println("Could not find frame number in Page Table "+fr);
        return(-1);
    }

    // *******************************************
    // The following method is provided for debugging purposes
    // Call it to display the various data structures defined
    // for the process so that you may examine the effect
    // of your page replacement algorithm on the state of the 
    // process.
    // Method for displaying the state of a process
    // *******************************************
    public static void logProcessState(Process prc)
    {
        int i;

        System.out.println("--------------Process "+prc.pid+"----------------");
        System.out.println("Virtual pages: Total: "+prc.numPages+
                " Code pages: "+prc.numCodePages+
                " Data pages: "+prc.numDataPages+
                " Stack pages: "+prc.numStackPages+
                " Heap pages: "+prc.numHeapPages);
        System.out.println("Simulation data: numAccesses left in cycle: "+prc.numMemAccess+
                " Num to next change in working set: "+prc.numMA2ChangeWS);
        System.out.println("Working set is :");
        for(i=0 ; i<prc.workingSet.length; i++)
        {
            System.out.print(" "+prc.workingSet[i]);
        }
        System.out.println();
        // page Table
        System.out.println("Page Table");
        if(prc.pageTable != null)
        {
            for(i=0 ; i<prc.pageTable.length ; i++)
            {
                if(prc.pageTable[i].valid) // its valid printout the data
                {
                    System.out.println("   Page "+i+"(valid): "+
                            " Frame "+prc.pageTable[i].frameNum+
                            " Used "+prc.pageTable[i].used+
                            " count "+prc.pageTable[i].count+
                            " Time Stamp "+prc.pageTable[i].tmStamp);
                }
                else System.out.println("   Page "+i+" is invalid (i.e not loaded)");
            }
        }
        // allocated frames
        System.out.println("Allocated frames (max is "+prc.numAllocatedFrames+")"+
                " (frame pointer is "+prc.framePtr+")");
        if(prc.allocatedFrames != null)
        {
            for(i=0 ; i<prc.allocatedFrames.length ; i++)
                System.out.print(" "+prc.allocatedFrames[i]);
        }
        System.out.println();
        System.out.println("---------------------------------------------");
    }
}
