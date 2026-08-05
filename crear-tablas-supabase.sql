-- ============================================
-- Script SQL para crear tablas en Supabase
-- Sistema: Kriolos POS
-- ============================================

-- ============================================
-- 1. TABLA: productos
-- ============================================
CREATE TABLE IF NOT EXISTS productos (
    id TEXT PRIMARY KEY,
    referencia TEXT,
    codigo TEXT,
    tipocodigobarras TEXT,
    nombre TEXT NOT NULL,
    preciocompra NUMERIC(15, 2) DEFAULT 0.0,
    precioventa NUMERIC(15, 2) DEFAULT 0.0,
    categoriaid TEXT,
    categorianombre TEXT,
    categoriaimpuesto TEXT,
    categoriaimpuestonombre TEXT,
    atributos TEXT,
    tieneimagen BOOLEAN DEFAULT false,
    escompuesto BOOLEAN DEFAULT false,
    imprimirencocina BOOLEAN DEFAULT false,
    estadoenvio BOOLEAN DEFAULT false,
    fechaextraccion TIMESTAMP,
    tabla TEXT DEFAULT 'products',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_productos_categoria ON productos(categoriaid);
CREATE INDEX IF NOT EXISTS idx_productos_nombre ON productos(nombre);
CREATE INDEX IF NOT EXISTS idx_productos_codigo ON productos(codigo);


-- ============================================
-- 2. TABLA: usuarios
-- ============================================
CREATE TABLE IF NOT EXISTS usuarios (
    id TEXT PRIMARY KEY,
    nombre TEXT,
    tarjeta TEXT,
    card TEXT, -- Alias para compatibilidad
    rol TEXT,
    visible BOOLEAN DEFAULT true,
    tieneimagen BOOLEAN DEFAULT false,
    fechaextraccion TIMESTAMP,
    tabla TEXT DEFAULT 'people',
    sucursal_nombre TEXT,
    sucursal_direccion TEXT,
    apppassword TEXT,
    image TEXT, -- Base64
    branch_name TEXT,
    branch_address TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_usuarios_tarjeta ON usuarios(tarjeta);
CREATE INDEX IF NOT EXISTS idx_usuarios_card ON usuarios(card);
CREATE INDEX IF NOT EXISTS idx_usuarios_rol ON usuarios(rol);

-- ============================================
-- 3. TABLA: clientes
-- ============================================
CREATE TABLE IF NOT EXISTS clientes (
    id TEXT PRIMARY KEY,
    codigobusqueda TEXT,
    numeroidentificacion TEXT,
    nombre TEXT,
    tarjeta TEXT,
    categoriaimpuesto TEXT,
    primernombre TEXT,
    apellido TEXT,
    email TEXT,
    telefono TEXT,
    telefono2 TEXT,
    direccion TEXT,
    direccion2 TEXT,
    codigopostal TEXT,
    ciudad TEXT,
    region TEXT,
    pais TEXT,
    fecharegistro TIMESTAMP,
    deudaactual NUMERIC(15, 2) DEFAULT 0.0,
    deudamaxima NUMERIC(15, 2) DEFAULT 0.0,
    visible BOOLEAN DEFAULT true,
    fechaextraccion TIMESTAMP,
    tabla TEXT DEFAULT 'customers',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_clientes_tarjeta ON clientes(tarjeta);
CREATE INDEX IF NOT EXISTS idx_clientes_nombre ON clientes(nombre);
CREATE INDEX IF NOT EXISTS idx_clientes_email ON clientes(email);

-- ============================================
-- 4. TABLA: categorias
-- ============================================
CREATE TABLE IF NOT EXISTS categorias (
    id TEXT PRIMARY KEY,
    nombre TEXT NOT NULL,
    categoriapadre TEXT,
    tieneimagen BOOLEAN DEFAULT false,
    fechaextraccion TIMESTAMP,
    tabla TEXT DEFAULT 'categories',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_categorias_padre ON categorias(categoriapadre);

-- ============================================
-- 5. TABLA: impuestos
-- ============================================
CREATE TABLE IF NOT EXISTS impuestos (
    id TEXT PRIMARY KEY,
    nombre TEXT NOT NULL,
    categoria TEXT,
    tasa NUMERIC(10, 4) DEFAULT 0.0,
    tipo TEXT DEFAULT 'impuesto',
    fechaextraccion TIMESTAMP,
    tabla TEXT DEFAULT 'taxes',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_impuestos_categoria ON impuestos(categoria);

-- ============================================
-- 6. TABLA: ventas
-- ============================================
CREATE TABLE IF NOT EXISTS ventas (
    id TEXT PRIMARY KEY,
    caja TEXT,
    fechaventa TIMESTAMP WITH TIME ZONE,
    vendedorid TEXT,
    vendedornombre TEXT,
    total NUMERIC(15, 2) DEFAULT 0.0,
    numerolineas INTEGER DEFAULT 0,
    lineas JSONB, -- Array de líneas de la venta
    fechaextraccion TIMESTAMP WITH TIME ZONE,
    tabla TEXT DEFAULT 'receipts',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ventas_fechaventa ON ventas(fechaventa);
CREATE INDEX IF NOT EXISTS idx_ventas_vendedorid ON ventas(vendedorid);
CREATE INDEX IF NOT EXISTS idx_ventas_caja ON ventas(caja);
CREATE INDEX IF NOT EXISTS idx_ventas_lineas ON ventas USING GIN(lineas);

-- ============================================
-- 7. TABLA: cierres
-- ============================================
CREATE TABLE IF NOT EXISTS cierres (
    id TEXT PRIMARY KEY,
    dineroid TEXT,
    dineromonto NUMERIC(15, 2) DEFAULT 0.0,
    host TEXT,
    secuencia INTEGER,
    fechainicio TIMESTAMP,
    fechafin TIMESTAMP,
    initial_amount NUMERIC(15, 2),
    faltante_cierre NUMERIC(15, 2),
    sobrante_cierre NUMERIC(15, 2),
    fechaextraccion TIMESTAMP,
    fechasincronizacion TIMESTAMP,
    origen TEXT DEFAULT 'kriolos-pos',
    version TEXT DEFAULT '1.0',
    tabla TEXT DEFAULT 'closedcash',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cierres_dineroid ON cierres(dineroid);
CREATE INDEX IF NOT EXISTS idx_cierres_fechafin ON cierres(fechafin);
CREATE INDEX IF NOT EXISTS idx_cierres_host ON cierres(host);

-- ============================================
-- 8. TABLA: puntos_historial
-- ============================================
CREATE TABLE IF NOT EXISTS puntos_historial (
    id TEXT PRIMARY KEY,
    clienteid TEXT,
    clientenombre TEXT,
    clientetarjeta TEXT,
    puntosactuales INTEGER,
    puntostotales INTEGER,
    puntosotorgados INTEGER DEFAULT 0,
    descripcion TEXT,
    montocompra NUMERIC(15, 2) DEFAULT 0.0,
    ultimatransaccion TEXT,
    fechaultimatransaccion TIMESTAMP,
    fechacreacion TIMESTAMP,
    fechatransaccion TIMESTAMP,
    tipo TEXT DEFAULT 'historial_puntos',
    fechaextraccion TIMESTAMP,
    tabla TEXT DEFAULT 'puntos_historial',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_puntos_clienteid ON puntos_historial(clienteid);
CREATE INDEX IF NOT EXISTS idx_puntos_tarjeta ON puntos_historial(clientetarjeta);
CREATE INDEX IF NOT EXISTS idx_puntos_fechatransaccion ON puntos_historial(fechatransaccion);

-- ============================================
-- 9. TABLA: formas_de_pago
-- ============================================
CREATE TABLE IF NOT EXISTS formas_de_pago (
    id TEXT PRIMARY KEY,
    recibo TEXT,
    metodopago TEXT,
    total NUMERIC(15, 2) DEFAULT 0.0,
    recibido NUMERIC(15, 2) DEFAULT 0.0,
    nombretarjeta TEXT,
    voucher TEXT,
    fechaventa TIMESTAMP WITH TIME ZONE,
    fechaextraccion TIMESTAMP,
    fechasincronizacion TIMESTAMP,
    origen TEXT DEFAULT 'kriolos-pos',
    version TEXT DEFAULT '1.0',
    tabla TEXT DEFAULT 'payments',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_formas_pago_recibo ON formas_de_pago(recibo);
CREATE INDEX IF NOT EXISTS idx_formas_pago_metodo ON formas_de_pago(metodopago);
CREATE INDEX IF NOT EXISTS idx_formas_pago_fechaventa ON formas_de_pago(fechaventa);

-- ============================================
-- 10. TABLA: config
-- ============================================
CREATE TABLE IF NOT EXISTS config (
    id TEXT PRIMARY KEY,
    montoporpunto NUMERIC(15, 2),
    puntosotorgados INTEGER,
    sistemaactivo BOOLEAN DEFAULT false,
    moneda TEXT,
    limitediario INTEGER,
    fechacreacion TIMESTAMP,
    fechaactualizacion TIMESTAMP,
    nombre TEXT, -- Para roles
    permisos TEXT, -- Para roles
    tipo TEXT, -- 'configuracion_puntos' o 'rol'
    fechaextraccion TIMESTAMP,
    tabla TEXT, -- 'puntos_configuracion' o 'roles'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_config_tipo ON config(tipo);
CREATE INDEX IF NOT EXISTS idx_config_tabla ON config(tabla);

-- ============================================
-- 11. TABLA: inventario
-- ============================================
CREATE TABLE IF NOT EXISTS inventario (
    id TEXT PRIMARY KEY,
    fecha TIMESTAMP,
    razon TEXT,
    ubicacion TEXT,
    productoid TEXT,
    productonombre TEXT,
    productoreferencia TEXT,
    atributos TEXT,
    unidades NUMERIC(15, 4) DEFAULT 0.0,
    precio NUMERIC(15, 2),
    tipo TEXT, -- 'stock_actual' o 'movimiento_stock'
    fechaextraccion TIMESTAMP,
    tabla TEXT, -- 'stockcurrent' o 'stockdiary'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inventario_productoid ON inventario(productoid);
CREATE INDEX IF NOT EXISTS idx_inventario_ubicacion ON inventario(ubicacion);
CREATE INDEX IF NOT EXISTS idx_inventario_tipo ON inventario(tipo);
CREATE INDEX IF NOT EXISTS idx_inventario_fecha ON inventario(fecha);

-- ============================================
-- COMENTARIOS EN TABLAS
-- ============================================
COMMENT ON TABLE productos IS 'Productos del sistema POS';
COMMENT ON TABLE usuarios IS 'Usuarios del sistema POS';
COMMENT ON TABLE clientes IS 'Clientes del sistema POS';
COMMENT ON TABLE categorias IS 'Categorías de productos';
COMMENT ON TABLE impuestos IS 'Impuestos y tasas';
COMMENT ON TABLE ventas IS 'Ventas realizadas';
COMMENT ON TABLE cierres IS 'Cierres de caja';
COMMENT ON TABLE puntos_historial IS 'Historial de puntos de clientes';
COMMENT ON TABLE formas_de_pago IS 'Formas de pago utilizadas';
COMMENT ON TABLE config IS 'Configuraciones del sistema';
COMMENT ON TABLE inventario IS 'Inventario y movimientos de stock';

-- ============================================
-- FIN DEL SCRIPT
-- ============================================
