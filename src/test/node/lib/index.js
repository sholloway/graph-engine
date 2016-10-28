const path = require('path');
const spawn = require('child_process').spawn;

const wd = path.join(__dirname, '../../../../target/scala-2.11')
const args = ['-cp',
  'machine-engine-assembly-0.1.0-deps.jar:machine-engine-assembly-0.1.0.jar',
  'org.machine.engine.Main'];

const opts = {
  cwd: wd //Current working directory of the child process (Engine).
};
const engine = spawn('java', args, opts);

engine.stdout.on('data', (data) => {
  var msg = data.toString()
  switch(msg.trim()){
    case 'ENGINE_READY':{
      console.log(`Node Received: ${msg}`);
      setTimeout(doWork, 2000) //Invoke in 5 seconds
      // doWork();
      break;
    }
    default:{
      console.log(`Node stdout: ${msg}`);
    }
  }
});

engine.stderr.on('data', (data) => {
  console.log(`Node stderr: ${data}`);
});

engine.on('close', (code) => {
  console.log(`Node child process exited with code ${code}`);
  process.exit() //kill the node harness.
});

/*
Challenges:
* [ ] Need to pass a application.conf file to the Engine child process.
* [ ] Need to be able to specify the logger level.
* [ ] Need a callback for when the Engine is fully ready.
* [ ] Need to be able to shut down the engine.
*/

function doWork(){
  console.log("Doing Work...")
  engine.stdin.setEncoding('utf-8');
  engine.stdin.write("Hello\n")
  engine.stdin.write("How are you?\n")
  engine.stdin.write("You should shutdown now.\n")
  engine.stdin.write("SIGHUP\n")
  engine.stdin.end()
  console.log("Done with Work...")
  // engine.kill()
}
