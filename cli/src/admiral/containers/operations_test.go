package containers

import (
	"testing"
)

//Test if start command is working properly.
//Will provision 2, stop them then start them.
//Running containers should be 2.
func TestStartCommand(t *testing.T) {
	preparation()
	lc := ListContainers{}
	cd := sampleContainerDescription()
	//Provisioning 2 containers.
	for i := 0; i < 2; i++ {
		cd.RunContainer(true)
	}

	//Fetching containers.
	lc.FetchContainers("")
	//Stopping all, skipping if it's system container.
	for _, i := range lc.Documents {
		if i.System {
			continue
		}
		StopContainer(i.Names, true)
	}
	//Fetching containers.
	lc.FetchContainers("")
	//Starting all, skipping it's system container.
	for _, i := range lc.Documents {
		if i.System {
			continue
		}
		StartContainer(i.Names, true)
	}

	expected := 2
	actual := 0
	//Fetching containers, and counting the running ones.
	lc.FetchContainers("")
	for _, i := range lc.Documents {
		if i.PowerState == "RUNNING" && !i.System {
			actual++
		}
	}
	//Assert
	if actual != expected {
		t.Errorf("Expected containers %d, actual containers %d", expected, actual)
	}
}

//Test if stop command is working.
//Will provision 2 containers, then stop them.
//Stopped containers should be 2.
func TestStopCommand(t *testing.T) {
	preparation()
	lc := ListContainers{}
	cd := sampleContainerDescription()
	for i := 0; i < 2; i++ {
		cd.RunContainer(true)
	}

	lc.FetchContainers("")
	for _, i := range lc.Documents {
		if i.System {
			continue
		}
		StopContainer(i.Names, true)
	}

	expected := 2
	actual := 0
	lc.FetchContainers("")
	for _, i := range lc.Documents {
		if i.PowerState == "STOPPED" && !i.System {
			actual++
		}
	}

	if actual != expected {
		t.Errorf("Expected containers %d, actual containers %d", expected, actual)
	}
}