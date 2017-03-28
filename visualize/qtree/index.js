'use strict';
// Top left: [40,9, -74.25]
// Bottom right: [40.5, -73.7]

window.initMap = function() {
    d3.json("qtree.json", function (error, data) {
        if (error) throw error;
        var google = window.google;
        function SVGOverlay (map) {
            this.map = map;
            this.canvas = null;
            this.quadtree = null;

            this.onPan = this.onPan.bind(this);

            this.setMap(map);
        }

        SVGOverlay.prototype = new google.maps.OverlayView();

        SVGOverlay.prototype.onAdd = function () {
            let base = d3.select("#vis");
            this.canvas = base.append("canvas")
                .style("position", "absolute")
                .style("top", 0)
                .style("left", 0)
                .style("width", "100%")
                .style("height", "100%")
                .style("pointer-events", "none");

            const bounds = this.map.getBounds();
            const proj = this.getProjection();

            this.quadtree = {};
            let maxLevel = 0
            data.forEach((d) => {
                if (!this.quadtree[d.depth]) {
                    this.quadtree[d.depth] = [];
                    if (d.depth > maxLevel) {
                        maxLevel = d.depth;
                    }
                }
                this.quadtree[d.depth].push(d);
            });
            this.quadtree.maxLevel = maxLevel;
            this.map.addListener('center_changed', this.onPan);
        };

        SVGOverlay.prototype.onPan = function () {
            if (this.panTimeout) {
                clearTimeout(this.panTimeout);
            }
            if (this.zoomTimeout) {
                clearTimeout(this.zoomTimeout);
            }
            this.panTimeout = timeoutDraw(this)
        };

        SVGOverlay.prototype.onRemove = function () {
            this.canvas.removeListener('center_changed', this.onPan);
            this.canvas.parentNode.removeChild(this.canvas);
            this.canvas = null;
        };

        SVGOverlay.prototype.draw = function () {
            if (this.panTimeout) {
                clearTimeout(this.panTimeout);
            }
            if (this.zoomTimeout) {
                clearTimeout(this.zoomTimeout);
            }
            this.zoomTimeout = timeoutDraw(this)
        };

        // Create the Google Map…
        const map = new google.maps.Map(d3.select("#map").node(), {
            zoom: 11,
            center: new google.maps.LatLng(40.7, -73.975),
            mapTypeId: google.maps.MapTypeId.TERRAIN,
            styles: [
                {elementType: 'geometry', stylers: [{color: '#242f3e'}]},
                {elementType: 'labels.text.stroke', stylers: [{color: '#242f3e'}]},
                {elementType: 'labels.text.fill', stylers: [{color: '#746855'}]},
                {
                    featureType: 'administrative.locality',
                    elementType: 'labels.text.fill',
                    stylers: [{color: '#d59563'}]
                },
                {
                    featureType: 'poi',
                    elementType: 'labels.text.fill',
                    stylers: [{color: '#d59563'}]
                },
                {
                    featureType: 'poi.park',
                    elementType: 'geometry',
                    stylers: [{color: '#263c3f'}]
                },
                {
                    featureType: 'poi.park',
                    elementType: 'labels.text.fill',
                    stylers: [{color: '#6b9a76'}]
                },
                {
                    featureType: 'road',
                    elementType: 'geometry',
                    stylers: [{color: '#38414e'}]
                },
                {
                    featureType: 'road',
                    elementType: 'geometry.stroke',
                    stylers: [{color: '#212a37'}]
                },
                {
                    featureType: 'road',
                    elementType: 'labels.text.fill',
                    stylers: [{color: '#9ca5b3'}]
                },
                {
                    featureType: 'road.highway',
                    elementType: 'geometry',
                    stylers: [{color: '#746855'}]
                },
                {
                    featureType: 'road.highway',
                    elementType: 'geometry.stroke',
                    stylers: [{color: '#1f2835'}]
                },
                {
                    featureType: 'road.highway',
                    elementType: 'labels.text.fill',
                    stylers: [{color: '#f3d19c'}]
                },
                {
                    featureType: 'transit',
                    elementType: 'geometry',
                    stylers: [{color: '#2f3948'}]
                },
                {
                    featureType: 'transit.station',
                    elementType: 'labels.text.fill',
                    stylers: [{color: '#d59563'}]
                },
                {
                    featureType: 'water',
                    elementType: 'geometry',
                    stylers: [{color: '#17263c'}]
                },
                {
                    featureType: 'water',
                    elementType: 'labels.text.fill',
                    stylers: [{color: '#515c6d'}]
                },
                {
                    featureType: 'water',
                    elementType: 'labels.text.stroke',
                    stylers: [{color: '#17263c'}]
                }
            ]
        });

        let overlay = new SVGOverlay(map);
    });
};

function timeoutDraw(ctx) {
    // Resize canvas to screen size
    ctx.canvas
        .attr("width", ctx.canvas.node().offsetWidth)
        .attr("height", ctx.canvas.node().offsetHeight);

    // Get projection and map bounds
    const proj = ctx.getProjection();
    const bounds = ctx.map.getBounds();
    const zoom = ctx.map.getZoom();

    // clear the canvas from previous drawing
    let cWidth = ctx.canvas.node().width;
    let cHeight = ctx.canvas.node().height;
    ctx.canvas.node().getContext('2d').clearRect(0, 0, cWidth, cHeight);

    return setTimeout(function() {
        paintCanvas(ctx.canvas, ctx.quadtree, proj, bounds, zoom);
    }, 50)
}

function paintCanvas(canvas, data, proj, bounds, zoom) {
    // get the canvas drawing context and the width and height
    const context = canvas.node().getContext('2d');
    let cWidth = canvas.node().width;
    let cHeight = canvas.node().height;

    // clear the canvas from previous drawing
    context.clearRect(0, 0, cWidth, cHeight);

    // Draw all rects
    for (const level in data) {
        if (data.hasOwnProperty(level)) {
            if (level < zoom) {
                const quads = data[level];
                context.fillStyle = "rgba(183,28,28, " + (0.4 * (level / data.maxLevel) * (level / data.maxLevel)) + ")";
                context.beginPath();
                quads.forEach((d) => {
                    if (bounds.contains(new google.maps.LatLng(d.x1, d.y1)) ||
                        bounds.contains(new google.maps.LatLng(d.x2, d.y1)) ||
                        bounds.contains(new google.maps.LatLng(d.x1, d.y2)) ||
                        bounds.contains(new google.maps.LatLng(d.x2, d.y2))
                    ) {
                        const p1 = transformXY(proj, d.x1, d.y1);
                        const p2 = transformXY(proj, d.x2, d.y2);

                        context.rect(Math.ceil(p1.x), Math.ceil(p2.y), Math.ceil(p2.x - p1.x), Math.ceil(p1.y - p2.y));
                    }
                });
                context.fill();
            }
        }
    }

}


function transformXY(proj, x, y) {
    return proj.fromLatLngToContainerPixel(new google.maps.LatLng(x, y));
}