# om2m.ipe.flicio
Eclipse oM2M Shorcutlabs Flic.io Interworking Proxy Entity

#Complementary onfiguration for the Eclipse oM2M config.ini
http://projects.eclipse.org/projects/technology.om2m

#Add the following reference to the "osgi.bundles="
reference\:file\:org.eclipse.om2m.ipe.flicio-1.0.0-SNAPSHOT.jar@4

#Configure the deamon URL thru the
#IP address of the remote Flic.io BLE Deamon
org.eclipse.om2m.ipe.flicio.flicd.ip=127.0.0.1
#TCP port of the remote Flic.io BLE Deamon
org.eclipse.om2m.ipe.flicio.flicd.port=5551
