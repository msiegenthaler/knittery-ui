THREE = require('three')
TrackballControls = require('three.trackball')
stitchRender = require('./stitch-render')
cutpiece = require('./cutpiece')

module.exports = (m) ->

  createScene = ->
    scene = new THREE.Scene()
    scene.add(new THREE.AmbientLight(0xcccccc))
    scene.fog = new THREE.FogExp2(0x000000, 0.035)
    pointLight = new THREE.PointLight(0xff4444, 5, 30)
    pointLight.position.set(5, 0, 0)
    scene.add(pointLight)
    camera = new THREE.PerspectiveCamera(50, 1, 1, 2000)
    camera.position.x = 2
    camera.position.y = 2
    camera.position.z = 2
    renderer = new THREE.WebGLRenderer({alpha: true})
    renderer.setPixelRatio(window.devicePixelRatio)
    [scene, camera, renderer]

  createControl = (camera, elem) ->
    controls = new TrackballControls(camera, elem)
    controls.rotateSpeed = 1.0
    controls.zoomSpeed = 1.2
    controls.panSpeed = 0.8
    controls.noZoom = false
    controls.noPan = false
    controls.staticMoving = true
    controls.dynamicDampingFactor = 0.3
    controls.keys = [65, 83, 68]
    controls

  makeTextureCanvas = (knitting) ->
    stitchSize = 10
    data = stitchRender.parseJson(knitting, stitchSize)
    emptyStitch = new stitchRender.emptyStitch(stitchSize)
    effective = cutpiece.effectiveKnitted(cutpiece.create(data.mainBed))
    #Front
    frontAll = cutpiece.betweenMarkedRows(effective, null, 'front/bottom')
    front = cutpiece.betweenMarkedColumns(frontAll, 'left-side', 'right-side')
    frontLeft = cutpiece.betweenMarkedColumns(frontAll, 'hidden', 'left-side', false, true)
    frontRight = cutpiece.betweenMarkedColumns(frontAll, 'right-side', 'hidden', true, false)
    #Bottom
    bottomAll = cutpiece.betweenMarkedRows(effective, 'front/bottom', 'bottom/back')
    bottom = cutpiece.betweenMarkedColumns(bottomAll, 'left-side', 'right-side')
    #Back
    backAll = cutpiece.betweenMarkedRows(effective, 'bottom/back', 'back/lash').mirrorRows()
    back = cutpiece.betweenMarkedColumns(backAll, 'left-side', 'right-side')
    backLeft = cutpiece.betweenMarkedColumns(backAll, 'hidden', 'left-side', false, true)
    backRight = cutpiece.betweenMarkedColumns(backAll, 'right-side', 'hidden', true, false)
    #Lash
    lash = cutpiece.effectiveBetweenMarkedRows(effective, 'back/lash', undefined, true, false, 'hidden')
    #Left/Right
    left = cutpiece.composeHorizontal(frontLeft, backLeft, emptyStitch).mirrorColumns()
    right = cutpiece.composeHorizontal(frontRight, backRight, emptyStitch)

    draw = (what) -> (ctx) ->
      ctx.save()
      ctx.scale(1 / stitchSize / what.width(), 1 / stitchSize / what.height())
      stitchRender.renderStitches(ctx, stitchSize)(what)
      ctx.restore()
    drawRotated = (what) -> (ctx) ->
      ctx.save()
      ctx.rotate(Math.PI / 2)
      ctx.translate(0, -1)
      ctx.scale(1 / stitchSize / what.width(), 1 / stitchSize / what.height())
      stitchRender.renderStitches(ctx, stitchSize)(what)
      ctx.restore()
    inside = (ctx) ->
      ctx.fillStyle = 'grey'
      ctx.fillRect(0, 0, 1, 1)

    #render: (ctx) -> {effect: draws the thing into the context into the rect [0,0,1,1]}
    parts = [
      {render: draw(left), width: 25},
      {render: draw(front), width: 500},
      {render: draw(right), width: 25},
      {render: draw(back), width: 500},
      {render: drawRotated(bottom), width: 25},
      {render: draw(lash), width: 500},
      {render: inside, width: 25}]

    canvas = document.createElement("canvas")
    ctx = canvas.getContext("2d")
    canvas.height = 700
    canvas.width = (p.width for p in parts).reduce((x, y)-> x + y)

    ctx.save()
    for part in parts
      ctx.save()
      ctx.scale(part.width, canvas.height)
      part.render(ctx)
      ctx.restore()
      ctx.translate(part.width, 0)
    ctx.restore()
    canvas

  loader = new THREE.JSONLoader()


  # shows 3d model of the resulting product using Three.js
  m.directive('knittingView3d', ($window) ->
    scope:
      knitting: '='
      modelUrl: '='

    link: (scope, elem) ->
      [scene, camera, renderer] = createScene()
      controls = createControl(camera, elem[0])
      elem.append(renderer.domElement)

      resize = ->
        renderer.setSize(elem.width(), elem.height())
        camera.aspect = elem.width() / elem.height()
        camera.updateProjectionMatrix()
        controls.handleResize()
      $($window).resize(resize)
      resize()

      render = ->
        camera.lookAt(scene.position)
        renderer.render(scene, camera)
      animate = ->
        $window.requestAnimationFrame(animate)
        controls.update()
        render()
      animate()

      modelAdded = false
      texture = null
      model = null

      updateModel = ->
        if scope.knitting?
          texture = new THREE.Texture(makeTextureCanvas(scope.knitting))
          texture.needsUpdate = true
          model.material.map = texture
          if not modelAdded
            scene.add(model)
            modelAdded = true

      scope.$watch('knitting', updateModel)
      scope.$watch('modelUrl', (url) ->
        if url?
          loader.load(url, (geometry, materials) -> scope.$apply(->
            model = new THREE.Mesh(geometry, materials[0])
          ))
        else
          model = null
        updateModel()
      )
  )

  # Shows the texture that will be used for the 3d model
  m.directive('knittingView3dTexture', ($window) ->
    replace: true
    scope:
      knitting: '='

    template: """
      <canvas style="width: 100%; height: height: 100%"></canvas>
      """

    link: (scope, elem) ->
      canvas = elem[0]
      ctx = canvas.getContext("2d")
      canvas.width = 1
      canvas.height = 1
      draw = () ->
        ctx.clearRect(0, 0, canvas.width, canvas.height)
        if scope.knitting
          cvs = makeTextureCanvas(scope.knitting)
          canvas.width = cvs.width
          canvas.height = cvs.height
          ctx.drawImage(cvs, 0, 0)

      scope.$watch('knitting', draw)
      $($window).resize(draw)
  )
