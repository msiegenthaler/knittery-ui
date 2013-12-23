$(() ->
  req = jsRoutes.controllers.Display.positions().ajax({
    success: (data) ->
      setPosition(carriage, position) for carriage, position of data.positions
      setRow(data.row)
  })

  ws = new WebSocket(jsRoutes.controllers.Display.subscribe().webSocketURL())
  ws.onmessage = (msg) ->
    parsed = $.parseJSON(msg.data)
    updateFrom(parsed)
)

updateFrom = (msg) ->
  switch msg.event
    when "positionChange"
      setPosition(msg.carriage, msg.position)
      setRow(msg.row)

setRow = (row) ->
  $(".row-position .positions-value").text(row)
  
setPosition = (carriage, position) ->
  [needle,text] = switch position.where
    when "left"    then [0, "-#{position.overlap} left"]
    when "right"   then [199, "-#{position.overlap} right"]
    when "needles" then [position.index, position.needle]
    else ""

  $("#"+carriage+"-position .positions-value").text(text)

  $(".graphical .carriage-type").text("Carriage (#{carriage})")

  bar = $("#bar .progress-bar")
  color = switch carriage
    when "K" then "info"
    when "L" then "success"
    when "G" then "warning"
  bar.removeClass("progress-bar-#{c}") for c in ["info", "warning", "success"]
  bar.addClass("progress-bar-#{color}")
  bar.attr("aria-valuenow", needle)
  bar.width((needle*100/199) + "%")
  bar.find("span.sr-only").text(text)

  $(".graphical .needle-pos").text(switch position.where
    when "needles" then position.needle
    else position.where
  )

$(() ->
  $(".needles").needles(200)
)
