# md-cst-montecarlo
Monte Carlo Simulations plugin for MagicDraw's Cameo Simulation Toolkit
There are two main files that may be of interest:
 
Src/.../actions/ConvertActivityAction.java
Src/...…/actions/RunMonteCarloAction.java
 
The first is used to convert CallActions into CallOperationActions (and creates required corresponding activities, etc.). The second is a simple invocation of Cameo Simulation Toolkit in a loop (there’s a check for the simulation run having ended, since the call is non-blocking).
 
It also includes a SysML model, doing the same thing, just without a plugin as support. 
Have a look at Data/(block)MonteCarlo/(activity)MonteCarlo, and particularly the specification of the OpaqueAction "Execute Simulation". It contains the following code:
 
runConfig = ALH.createObject(inObject);
session = com.nomagic.magicdraw.simulation.SimulationManager.execute(runConfig.getTypes().get(0), true, true);
print("Simulation running...");
while(!session.isClosed()) {     
    print("Simulation not yet done...");
    java.lang.Thread.sleep(1000);
}
 
Where "inObject" is a block with classifier behavior that should be executed in a loop. This worked in principle, but it also registers two simulation sessions, which seems to have messed up the simulation clock (the simulation just stopped).

