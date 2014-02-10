========================================================================
    STATIC LIBRARY : LibjingleFileTransfer Project Overview
========================================================================

AppWizard has created this LibjingleFileTransfer library project for you.

No source files were created as part of your project.


LibjingleFileTransfer.vcxproj
    This is the main project file for VC++ projects generated using an Application Wizard.
    It contains information about the version of Visual C++ that generated the file, and
    information about the platforms, configurations, and project features selected with the
    Application Wizard.

LibjingleFileTransfer.vcxproj.filters
    This is the filters file for VC++ projects generated using an Application Wizard. 
    It contains information about the association between the files in your project 
    and the filters. This association is used in the IDE to show grouping of files with
    similar extensions under a specific node (for e.g. ".cpp" files are associated with the
    "Source Files" filter).

/////////////////////////////////////////////////////////////////////////////
Notes:
In order to build libjingle-0.5.8:
1. Set PATH_TO_SWTOOLKIT=C:\SoftwareProjects\Java\Degoo\Client\Libjingle\libjingle-0.5.8\swtoolkit
2. Set SCONS_DIR=C:\SoftwareProjects\Java\Degoo\Client\Libjingle\libjingle-0.5.8\scons-local-2.0.1\scons-local-2.0.1
2. Following readme in libjingle-0.5.8.

A SWIG BUG need you manually workaround:
LibjingleDataTransfer_wrap.cxx file is generated from SWIG interface file. After generation, in method "SwigDirector_ReceiverCallback::onDataReceived(...)",
SWIG will build:
(jenv)->DeleteLocalRef(jb); 
in front of the method call:
jenv->CallStaticVoidMethod(Swig::jclass_LibjingleDataTransferJNI, Swig::director_methids[3], swigjobj, jBYTE, jlen, jremoteNodeID); 
So, there is a possiblity that data is deleted beofore dispatched to Java client. A workaround for this bug is to manually move (jenv)->DeleteLocalRef(jb);
after the JNI method call.

/////////////////////////////////////////////////////////////////////////////
