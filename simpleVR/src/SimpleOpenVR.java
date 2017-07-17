import jrtr.*;
import jrtr.glrenderer.*;

import javax.swing.*;

import java.awt.event.MouseListener;
import java.io.IOException;
import java.lang.reflect.Array;
import java.awt.event.MouseEvent;

import javax.vecmath.*;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Implements a simple VR application that renders to an HMD via
 * OpenVR. OpenVR functionality is provided by the {@link OpenVRRenderPanel}.
 * Also demonstrates tracking of a VR hand controller.
 */
public class SimpleOpenVR
{	
	static VRRenderPanel renderPanel;
	static RenderContext renderContext;
	static SimpleSceneManager sceneManager;
	
	//shapes
	static Shape ball;
	static Shape controllerCube;
	static Shape controllerCubeTriggered;
	static Shape surroundingCube;
	static Shape controllerRacket;
	static Shape testCube;
	static Shape person;
	
	//stores bounding box for racket. Useful for collision detection with ball.
	static Vector3f racketBoundsMax = new Vector3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
	static Vector3f racketBoundsMin = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
	
	//scene-geometry parameters
	static float ballRadius = 0.15f;
	static float roomSize = 2.f;
	static float controllerSize = 0.015f;

	//additional parameters
	static Vector3f throwingTranslationAccum;
	
	static final float GRAVITY = 0.001f;
	static final float FRICTION = 0.99f;
	static float bounds = 2;
	/**
	 * An extension of {@link OpenVRRenderPanel} to 
	 * provide a call-back function for initialization. 
	 */ 
	public final static class SimpleVRRenderPanel extends VRRenderPanel
	{
		private Timer timer;	// Timer to trigger animation rendering
		
		Matrix4f handTrafo = new Matrix4f();
		Matrix4f prevHandTrafo = new Matrix4f();
		
		Matrix4f racketTrafo = new Matrix4f();
		Matrix4f prevRacketTrafo = new Matrix4f();
		
		Matrix4f handGlobal = new Matrix4f();
		Matrix4f locTrafo = new Matrix4f();
		
		Vector3f ballVec = new Vector3f();
		Vector3f hitVec = new Vector3f();
		boolean holdingBall;
		boolean touchingRacket;
		
		float ballXVel = 0;
		float ballYVel = 0;
		
		float currentMag = 1;
		
		public Shape makeCube(){
			// Make a simple geometric object: a cube
			
			// The vertex positions of the cube
			float v[] = {-1,-1,1, 1,-1,1, 1,1,1, -1,1,1,		// front face
				         -1,-1,-1, -1,-1,1, -1,1,1, -1,1,-1,	// left face
					  	 1,-1,-1,-1,-1,-1, -1,1,-1, 1,1,-1,		// back face
						 1,-1,1, 1,-1,-1, 1,1,-1, 1,1,1,		// right face
						 1,1,1, 1,1,-1, -1,1,-1, -1,1,1,		// top face
						-1,-1,1, -1,-1,-1, 1,-1,-1, 1,-1,1};	// bottom face

			// The vertex normals 
			float n[] = {0,0,1, 0,0,1, 0,0,1, 0,0,1,			// front face
				         -1,0,0, -1,0,0, -1,0,0, -1,0,0,		// left face
					  	 0,0,-1, 0,0,-1, 0,0,-1, 0,0,-1,		// back face
						 1,0,0, 1,0,0, 1,0,0, 1,0,0,			// right face
						 0,1,0, 0,1,0, 0,1,0, 0,1,0,			// top face
						 0,-1,0, 0,-1,0, 0,-1,0, 0,-1,0};		// bottom face

			// The vertex colors
			float c[] = {1,0,0, 1,0,0, 1,0,0, 1,0,0,
					     0,1,0, 0,1,0, 0,1,0, 0,1,0,
						 1,0,0, 1,0,0, 1,0,0, 1,0,0,
						 0,1,0, 0,1,0, 0,1,0, 0,1,0,
						 0,0,1, 0,0,1, 0,0,1, 0,0,1,
						 0,0,1, 0,0,1, 0,0,1, 0,0,1};

			// Texture coordinates 
			float uv[] = {0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1};

			// Construct a data structure that stores the vertices, their
			// attributes, and the triangle mesh connectivity
			VertexData vertexData = renderContext.makeVertexData(24);
			vertexData.addElement(c, VertexData.Semantic.COLOR, 3);
			vertexData.addElement(v, VertexData.Semantic.POSITION, 3);
			vertexData.addElement(n, VertexData.Semantic.NORMAL, 3);
			vertexData.addElement(uv, VertexData.Semantic.TEXCOORD, 2);
			
			// The triangles (three vertex indices for each triangle)
			int indices[] = {0,2,3, 0,1,2,			// front face
							 4,6,7, 4,5,6,			// left face
							 8,10,11, 8,9,10,		// back face
							 12,14,15, 12,13,14,	// right face
							 16,18,19, 16,17,18,	// top face
							 20,22,23, 20,21,22};	// bottom face

			vertexData.addIndices(indices);

			return new Shape(vertexData);
		}
		
		/**
		 * Initialization call-back. We initialize our renderer here.
		 * 
		 * @param r	the render context that is associated with this render panel
		 */
		public void init(RenderContext r)
		{
			renderContext = r;
			
			// Make a simple geometric object: a cube
			
			// The vertex positions of the cube
			float v[] = {-1,-1,1, 1,-1,1, 1,1,1, -1,1,1,		// front face
				         -1,-1,-1, -1,-1,1, -1,1,1, -1,1,-1,	// left face
					  	 1,-1,-1,-1,-1,-1, -1,1,-1, 1,1,-1,		// back face
						 1,-1,1, 1,-1,-1, 1,1,-1, 1,1,1,		// right face
						 1,1,1, 1,1,-1, -1,1,-1, -1,1,1,		// top face
						-1,-1,1, -1,-1,-1, 1,-1,-1, 1,-1,1};	// bottom face
			//for(int i=0; i<Array.getLength(v); i++) v[i] = v[i] * 0.1f;	// make it smaller
			
			// The vertex colors
			float c[] = {0.5f,0,0, 0.5f,0,0, 0.5f,0,0, 0.5f,0,0,
					     0,0.3f,0, 0,0.3f,0, 0,0.3f,0, 0,0.3f,0,
					     0.5f,0,0, 0.5f,0,0, 0.5f,0,0, 0.5f,0,0,
					     0,0.3f,0, 0,0.3f,0, 0,0.3f,0, 0,0.3f,0,
					     0,0.3f,0.3f, 0,0.3f,0.3f, 0,0.3f,0.3f, 0,0.3f,0.3f,
					     0,0.3f,0.3f, 0,0.3f,0.3f, 0,0.3f,0.3f, 0,0.3f,0.3f,};
			
			// The vertex normals 
			float n[] = {0,0,1, 0,0,1, 0,0,1, 0,0,1,			// front face
				         -1,0,0, -1,0,0, -1,0,0, -1,0,0,		// left face
					  	 0,0,-1, 0,0,-1, 0,0,-1, 0,0,-1,		// back face
						 1,0,0, 1,0,0, 1,0,0, 1,0,0,			// right face
						 0,1,0, 0,1,0, 0,1,0, 0,1,0,			// top face
						 0,-1,0, 0,-1,0, 0,-1,0, 0,-1,0};		// bottom face

			// Texture coordinates 
			float uv[] = {0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1};
			
			// The triangles (three vertex indices for each triangle)
			int indices[] = {0,2,3, 0,1,2,			// front face
							 4,6,7, 4,5,6,			// left face
							 8,10,11, 8,9,10,		// back face
							 12,14,15, 12,13,14,	// right face
							 16,18,19, 16,17,18,	// top face
							 20,22,23, 20,21,22};	// bottom face
			
			// A room around the cube, made out of an other cube
			float[] vRoom = new float[Array.getLength(v)];
			float[] nRoom = new float[Array.getLength(n)];
			float[] cRoom = new float[Array.getLength(c)];
			for(int i=0; i<Array.getLength(vRoom); i++) vRoom[i] = v[i] * (roomSize);
			for(int i=0; i<Array.getLength(nRoom); i++) nRoom[i] = n[i] * -1.f;
			for(int i=0; i<Array.getLength(cRoom); i++) cRoom[i] = c[i] * 0.5f;			
						
			VertexData vertexDataRoom = renderContext.makeVertexData(24);
			vertexDataRoom.addElement(cRoom, VertexData.Semantic.COLOR, 3);
			vertexDataRoom.addElement(vRoom, VertexData.Semantic.POSITION, 3);			
			vertexDataRoom.addElement(nRoom, VertexData.Semantic.NORMAL, 3);
			vertexDataRoom.addElement(uv, VertexData.Semantic.TEXCOORD, 2);
			vertexDataRoom.addIndices(indices);
			
			// A smaller cube to show the tracked VR controller
			float[] vControllerCube = new float[Array.getLength(v)];
			float[] nControllerCube = new float[Array.getLength(n)];
			float[] cControllerCube = new float[Array.getLength(c)];
			for(int i=0; i<Array.getLength(vRoom); i++) vControllerCube[i] = v[i] * controllerSize;
			for(int i=0; i<Array.getLength(nRoom); i++) nControllerCube[i] = n[i];
			for(int i=0; i<Array.getLength(cRoom); i++) cControllerCube[i] = 0.4f;	
			VertexData vertexDataControllerCube = renderContext.makeVertexData(24);
			vertexDataControllerCube.addElement(cControllerCube, VertexData.Semantic.COLOR, 3);
			vertexDataControllerCube.addElement(vControllerCube, VertexData.Semantic.POSITION, 3);			
			vertexDataControllerCube.addElement(nControllerCube, VertexData.Semantic.NORMAL, 3);
			vertexDataControllerCube.addElement(uv, VertexData.Semantic.TEXCOORD, 2);
			vertexDataControllerCube.addIndices(indices);			
			
			// A smaller cube to show the tracked VR controller (brighter, when triggered)
			float[] cControllerCubeTriggered = new float[Array.getLength(c)];
			for(int i=0; i<Array.getLength(cRoom); i++) cControllerCubeTriggered[i] = 0.8f;	
			VertexData vertexDataControllerCubeTriggered = renderContext.makeVertexData(24);
			vertexDataControllerCubeTriggered.addElement(cControllerCubeTriggered, VertexData.Semantic.COLOR, 3);
			vertexDataControllerCubeTriggered.addElement(vControllerCube, VertexData.Semantic.POSITION, 3);			
			vertexDataControllerCubeTriggered.addElement(nControllerCube, VertexData.Semantic.NORMAL, 3);
			vertexDataControllerCubeTriggered.addElement(uv, VertexData.Semantic.TEXCOORD, 2);
			vertexDataControllerCubeTriggered.addIndices(indices);	
			
			// same controller cube with different colors, make it long and tin
			float[] vRacket = new float[Array.getLength(v)];
			for(int i=0; i<Array.getLength(vRoom)/3; i++){
				vRacket[3*i] =   controllerSize * v[3*i];
				vRacket[3*i+1] =   5.f * controllerSize * v[3*i+1];
				vRacket[3*i+2] =  20.f * controllerSize * v[3*i+2] - 0.2f;
				racketBoundsMax.x = Math.max(racketBoundsMax.x, vRacket[3*i]);
				racketBoundsMax.y = Math.max(racketBoundsMax.y, vRacket[3*i+1]);
				racketBoundsMax.z = Math.max(racketBoundsMax.z, vRacket[3*i+2]);
				racketBoundsMin.x = Math.min(racketBoundsMin.x, vRacket[3*i]);
				racketBoundsMin.y = Math.min(racketBoundsMin.y, vRacket[3*i+1]);
				racketBoundsMin.z = Math.min(racketBoundsMin.z, vRacket[3*i+2]);
			}
			VertexData vertexDataRacket = renderContext.makeVertexData(24);
			vertexDataRacket.addElement(cControllerCube, VertexData.Semantic.COLOR, 3);
			vertexDataRacket.addElement(vRacket, VertexData.Semantic.POSITION, 3);			
			vertexDataRacket.addElement(nControllerCube, VertexData.Semantic.NORMAL, 3);
			vertexDataRacket.addElement(uv, VertexData.Semantic.TEXCOORD, 2);
			vertexDataRacket.addIndices(indices);
			
			//The ball
			Sphere ballObj = new Sphere(30, ballRadius, new float[]{0.5f,0.4f,0.1f}, new float[]{0.2f,0.3f,0.5f});
			VertexData vertexDataBall = renderContext.makeVertexData(ballObj.n);
			vertexDataBall.addElement(ballObj.colors, VertexData.Semantic.COLOR, 3);
			vertexDataBall.addElement(ballObj.vertices, VertexData.Semantic.POSITION, 3);			
			vertexDataBall.addElement(ballObj.normals, VertexData.Semantic.NORMAL, 3);
			vertexDataBall.addElement(ballObj.texcoords, VertexData.Semantic.TEXCOORD, 2);
			vertexDataBall.addIndices(ballObj.indices);	
			
			// Make a scene manager and add the objects
			sceneManager = new SimpleSceneManager();
			
			surroundingCube 		= new Shape(vertexDataRoom);
			controllerCube 			= new Shape(vertexDataControllerCube);		
			controllerCubeTriggered = new Shape(vertexDataControllerCubeTriggered);
			controllerRacket = new Shape(vertexDataRacket);
			ball = new Shape(vertexDataBall);
			testCube = makeCube();
			
			try {
				person = makeObj("../obj/minato.obj", 1, r);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			sceneManager.addShape(surroundingCube);
			sceneManager.addShape(controllerCube);
			sceneManager.addShape(controllerCubeTriggered);
			sceneManager.addShape(controllerRacket);
			sceneManager.addShape(ball);
			
			
			//Set up objects
			
			throwingTranslationAccum = new Vector3f();
			
			// Set up the camera
			sceneManager.getCamera().setCenterOfProjection(new Vector3f(0,-1.f,0.2f));
			sceneManager.getCamera().setLookAtPoint(new Vector3f(0,-1.f,0));
			sceneManager.getCamera().setUpVector(new Vector3f(0,1,0));

			// Add the scene to the renderer
			renderContext.setSceneManager(sceneManager);
	
			resetBallPosition(); //set inital ball position
			
			//Shader
		    
			Shader defaultShader = renderContext.makeShader();
		    try {
		    	defaultShader.load("../jrtr/shaders/default.vert", "../jrtr/shaders/default.frag");
		    } catch(Exception e) {
		    	System.out.print("Problem with shader:\n");
		    	System.out.print(e.getMessage());
		    }
			
		    Shader toonShader = renderContext.makeShader();
		    try {
		    	toonShader.load("../jrtr/shaders/toon.vert", "../jrtr/shaders/toon.frag");
		    } catch(Exception e) {
		    	System.out.print("Problem with shader:\n");
		    	System.out.print(e.getMessage());
		    }
		    
		    Shader phongShader = renderContext.makeShader();
		    try {
		    	phongShader.load("../jrtr/shaders/phong.vert", "../jrtr/shaders/phong.frag");
		    } catch(Exception e) {
		    	System.out.print("Problem with shader:\n");
		    	System.out.print(e.getMessage());
		    }
		    
		    
		    // Make a material that can be used for shading
			
			
			Material swordMat = new Material();
			swordMat.shader = toonShader;
			swordMat.diffuseMap = renderContext.makeTexture();
			try {
				swordMat.diffuseMap.load("../textures/remote.png");
			} catch(Exception e) {				
				System.out.print("Could not load texture.\n");
				System.out.print(e.getMessage());
			}
			
		    // Make a material that can be used for shading
			Material roomMat = new Material();
			roomMat.shader = defaultShader;
			
			
			// Shader and material code
			surroundingCube.setMaterial(roomMat);
			ball.setMaterial(roomMat);
			controllerRacket.setMaterial(swordMat);
			
		    // Adds lights
		    Light l1 = new Light();
		    Light l2 = new Light();
		    
		    l1.position = new Vector3f(0, 0, 5);
		    l1.specular = new Vector3f(1, 0, 0);
		    l1.type = Light.Type.POINT;
		    
		    l2.position = new Vector3f(-5, -1, -1);
		    l2.specular = new Vector3f(1, 1, 1);
		    l2.type = Light.Type.POINT;
		    
		    
		    sceneManager.addLight(l1);
		    sceneManager.addLight(l2);
		    

		}
		
		public void dispose()
		{
			// Stop timer from triggering rendering of animation frames
			//timer.cancel();
			//timer.purge();
		}

		/*
		 * Helper function to visualise the controller corresponding to the hand.
		 * Gives visual feedback when trigger is pressed.
		 * Returns the trafo of the controller. 
		 */
		private Matrix4f visualizeHand(int index)
		{		
			Matrix4f handT = new Matrix4f();
			handT.setIdentity();

			if(index != -1) 
			{
				Matrix4f hiddenT = new Matrix4f();
    			Shape visibleShape, hiddenShape;
    			
    			// To have some feedback when pushing the trigger button we flip the two 
    			// "trigger" and "untrigger" shapes. The currently hidden object is 
    			// translated out of the viewfrustum since openGL does not have a direct 
    			// "make invisible" command for individual shapes w/o changing the jrtr
    			// pipeline.
    			if(renderPanel.getTriggerTouched(renderPanel.controllerIndexHand))
    			{
    				visibleShape = controllerCubeTriggered;
    				hiddenShape = controllerCube;
    			}
    			else
    			{
    				hiddenShape = controllerCubeTriggered;
    				visibleShape = controllerCube;	
    			}
    			
        		// Update pose of hand controller; note that the pose of the hand controller
        		// is independent of the scene camera pose, so we include the inverse scene
        		// camera matrix here to undo the camera trafo that is automatically applied
        		// by the renderer to all scene objects
    			handT = new Matrix4f(sceneManager.getCamera().getCameraMatrix());
    			handT.invert();
    			handT.mul(renderPanel.poseMatrices[index]);
	    		visibleShape.setTransformation(handT);
	    		
	    		//hidden shape is translated to "oblivion"
	    		hiddenT = new Matrix4f();
	    		hiddenT.setIdentity();
	    		hiddenT.setTranslation(new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE));
	    		hiddenShape.setTransformation(hiddenT);
	    		
    		}			
			return handT;
		}
		
		/*
		 * Helper function to visualise the controller corresponding to the racket.
		 * Returns the trafo of the controller. 
		 */
		private Matrix4f visualizeRacket(int index)
		{		
			Matrix4f racketT = new Matrix4f();
			racketT.setIdentity();
			if(index != -1) 
			{	
    			//current shape follows the controller
    			racketT = new Matrix4f(sceneManager.getCamera().getCameraMatrix());
    			racketT.invert();
    			racketT.mul(renderPanel.poseMatrices[index]);
    			
    			controllerRacket.setTransformation(racketT);
    		}			
			return racketT;
		}

		/*
		 * Helper function: Reset ball Position if we press the side buttons of the "hand"
		 */
		private void resetBallPosition()
		{
			//reset Ball Position
			Matrix4f ballInitTrafo = ball.getTransformation();
			ballInitTrafo.setIdentity();
			
			//reset all other class members related to remembering previous positions of objects	
			throwingTranslationAccum = new Vector3f(0,-0.7f,0); //shift ball a bit downwards since the camera is at 0,-1,-0.3
		}
		
		/*
		 * Override from base class. Triggered by 90 FPS animation.
		 */
		public void prepareDisplay()
		{
    		// Reset ball position
    		if(renderPanel.getSideTouched(renderPanel.controllerIndexHand) || renderPanel.getSideTouched(renderPanel.controllerIndexRacket))
    		{
    			resetBallPosition();	
    			ballVec = new Vector3f();
    		}
    		
			// get current ball transformation matrix.
    		Matrix4f ballTrafo = ball.getTransformation();		
    		
    		// Get VR tracked poses. Anything using any tracking data from the VR devices must happen *after* waitGetPoses() is called!
    		renderPanel.waitGetPoses();
    		
    		// Visualise controlling devices
    		prevHandTrafo = handTrafo;
    		handTrafo   = visualizeHand(renderPanel.controllerIndexHand);
    		prevRacketTrafo = racketTrafo;
    		racketTrafo = visualizeRacket(renderPanel.controllerIndexRacket);	
 
    		
    		// TODO: implement interaction with ball
    		
    		//Picks up ball
    		if(renderPanel.getTriggerTouched(renderPanel.controllerIndexHand) && pointInSphere(handTrafo))
			{
    			
    			if(!holdingBall){
    				
    				//haptic
        			renderPanel.triggerHapticPulse(controllerIndexHand, 1f);
    				
    				handGlobal = (Matrix4f) handTrafo.clone();
        			handGlobal.invert();
        			handGlobal.mul(ball.getTransformation());
        			
        			locTrafo = handGlobal;
    			}
    			
    	
    			
    			
    			ball.setTransformation(new Matrix4f());
    			ball.getTransformation().setIdentity();
    			
    			Matrix4f locClone = (Matrix4f) locTrafo.clone();
    			handGlobal = (Matrix4f) handTrafo.clone();
    			
    			
    			handGlobal.mul(locClone);
    			
    			ball.setTransformation(handGlobal);
    			
    			
    			//ball.setTransformation(handGlobal);
    			
				holdingBall = true;
			}
    		
    		//If ball was thrown
    		if(holdingBall && !renderPanel.getTriggerTouched(renderPanel.controllerIndexHand)){
    			ballVec = getHandMove();
    			holdingBall = false;
    			
    			currentMag = 1;
    		}
    		
    		//Intersect sphere
    		Matrix4f invRacket = (Matrix4f)racketTrafo.clone();
    		Matrix4f localBall = (Matrix4f)ball.getTransformation().clone();
    		invRacket.invert();
    		invRacket.mul(localBall);
    		localBall = invRacket;
    		
    		touchingRacket = checkIntersectRacket(localBall);
    		
    		if(touchingRacket){
    			
    			//haptic
    			renderPanel.triggerHapticPulse(controllerIndexRacket, 1f);
    			
    			ballVec = new Vector3f();
    			
    			//TEMP: DETECT SIDE
    			if(localBall.m03 > 0){
    				hitVec = new Vector3f(racketTrafo.m00, racketTrafo.m10, racketTrafo.m20);
    			}
    			else{
    				hitVec = new Vector3f(-racketTrafo.m00, -racketTrafo.m10, -racketTrafo.m20);
    			}
    			
    			hitVec.scale(0.01f);
    			hitVec.add(getRackMove());
    			
    			ballVec.add(hitVec);
    			
    			currentMag = 1;
    			
    		}
    		
    		updateBall();
    		
    		//update ball transformation matrix (right now this only shifts the ball a bit down)
    		ballTrafo.setTranslation(throwingTranslationAccum);
    		
    	}
		
		public void updateBall(){
			
			throwingTranslationAccum = new Vector3f(
					ball.getTransformation().m03,
					ball.getTransformation().m13,
					ball.getTransformation().m23);
		
			
			//Gravity
			if(ball.getTransformation().m13 - ballRadius > -bounds && !holdingBall && !touchingRacket){
				ballYVel -= GRAVITY;
				throwingTranslationAccum.add(new Vector3f(0, ballYVel, 0));
			}
			else{
				ballYVel = 0;
			}
			
			//Throw and hit
			throwingTranslationAccum.add(ballVec);
			ballVec.scale(currentMag * FRICTION);
			
			//Collision
			if(ball.getTransformation().m03 + ballRadius > bounds){
				ball.getTransformation().m03 = bounds - ballRadius;
				ballVec.x = -Math.abs(ballVec.x);
			}
			if(ball.getTransformation().m03 - ballRadius < -bounds){
				ball.getTransformation().m03 = -bounds + ballRadius;
				ballVec.x = Math.abs(ballVec.x);
			}
			if(ball.getTransformation().m13 + ballRadius > bounds){
				ball.getTransformation().m13 = bounds - ballRadius;
				ballVec.y = -Math.abs(ballVec.y);
			}
			if(ball.getTransformation().m13 - ballRadius < -bounds){
				ball.getTransformation().m13 = -bounds + ballRadius;
				ballVec.y = Math.abs(ballVec.y);
			}
			if(ball.getTransformation().m23 + ballRadius > bounds){
				ball.getTransformation().m23 = bounds - ballRadius;
				ballVec.z = -Math.abs(ballVec.z);
			}
			if(ball.getTransformation().m23 - ballRadius < -bounds){
				ball.getTransformation().m23 = -bounds + ballRadius;
				ballVec.z = Math.abs(ballVec.z);
			}
			
			throwingTranslationAccum.add(ballVec);
		}
		
		public Vector3f getHandMove(){
			Vector3f trans = new Vector3f(
					handTrafo.m03 - prevHandTrafo.m03,
					handTrafo.m13 - prevHandTrafo.m13,
					handTrafo.m23 - prevHandTrafo.m23
											);
			return trans;
		}
		
		public Vector3f getRackMove(){
			Vector3f trans = new Vector3f(
					racketTrafo.m03 - prevRacketTrafo.m03,
					racketTrafo.m13 - prevRacketTrafo.m13,
					racketTrafo.m23 - prevRacketTrafo.m23
											);
			return trans;
		}
		
		public boolean pointInSphere(Matrix4f t){
			Vector3f pt = new Vector3f(t.m03, t.m13, t.m23);
			Vector3f center = new Vector3f(
					ball.getTransformation().m03,
					ball.getTransformation().m13,
					ball.getTransformation().m23);
			
			double dist = Math.sqrt( Math.pow(pt.x-center.x,2) + Math.pow(pt.y-center.y,2) + Math.pow(pt.z-center.z,2) );
			return dist < ballRadius;
		}
		
		
		public boolean checkIntersectRacket(Matrix4f localBall){
			float x = Math.max(racketBoundsMin.x, Math.min(localBall.m03, racketBoundsMax.x));
			float y = Math.max(racketBoundsMin.y, Math.min(localBall.m13, racketBoundsMax.x));
			float z = Math.max(racketBoundsMin.z, Math.min(localBall.m23, racketBoundsMax.x));
			
			double dist = Math.sqrt( Math.pow(x-localBall.m03,2) + Math.pow(y-localBall.m13,2) + Math.pow(z-localBall.m23,2) );
			return dist < ballRadius;
		}
		
		
		
}

	
	/**
	 * The main function opens a 3D rendering window, constructs a simple 3D
	 * scene, and starts a timer task to generate an animation.
	 */
	public static void main(String[] args)
	{		
		// Make a render panel. The init function of the renderPanel
		// (see above) will be called back for initialization.
		renderPanel = new SimpleVRRenderPanel();
		
		// Make the main window of this application and add the renderer to it
		JFrame jframe = new JFrame("simple");
		jframe.setSize(1680, 1680);
		jframe.setLocationRelativeTo(null); // center of screen
		jframe.getContentPane().add(renderPanel.getCanvas());// put the canvas into a JFrame window

		// Add a mouse listener
	  //  renderPanel.getCanvas().addMouseListener(new SimpleMouseListener());
		renderPanel.getCanvas().setFocusable(true);
		
	    jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    jframe.setVisible(true); // show window
	    
	    
	}
	
	//Converts matrix to rotation axis
	public static Vector3f matrixToAxis(Matrix4f t){
		
		t = (Matrix4f)t.clone();
		t.setTranslation(new Vector3f(0, 0, 0));
		
		return new Vector3f(
					(float)((t.m21-t.m12)/Math.sqrt( Math.pow(t.m21-t.m12, 2)+Math.pow(t.m02-t.m20, 2)+Math.pow(t.m10-t.m01, 2) )),
					(float)((t.m02-t.m20)/Math.sqrt( Math.pow(t.m21-t.m12, 2)+Math.pow(t.m02-t.m20, 2)+Math.pow(t.m10-t.m01, 2) )),
					(float)((t.m10-t.m01)/Math.sqrt( Math.pow(t.m21-t.m12, 2)+Math.pow(t.m02-t.m20, 2)+Math.pow(t.m10-t.m01, 2) ))
				);
	}
	
	public static Vector3f matrixToEuler(Matrix4f t){
		
		t = (Matrix4f)t.clone();
		t.setTranslation(new Vector3f(0, 0, 0));
		
		float step = 180;
		float conv = (float)(step/ Math.PI);
		
		return new Vector3f(
					(float)Math.atan2(t.m21, t.m22) * conv + step,
					(float)Math.atan2(-t.m20, Math.sqrt( Math.pow(t.m21, 2)+Math.pow(t.m22, 2)) ) * conv,
					(float)Math.atan2(t.m10, t.m00) * conv + step
				);
	}
	
	public static Shape makeObj(String name, float scale, RenderContext r) throws IOException{
		VertexData vertexData = ObjReader.read(name, scale, r);
		
		//Color
		float[] c = new float[vertexData.getNumberOfVertices()*3];
		for(int i = 0; i < c.length; i+=3){
			c[i+0] = 1;
			c[i+1] = 1;
			c[i+2] = 1;
		}
						
		vertexData.addElement(c, VertexData.Semantic.COLOR, 3);
						
		return new Shape(vertexData);
		
	}
	
}
