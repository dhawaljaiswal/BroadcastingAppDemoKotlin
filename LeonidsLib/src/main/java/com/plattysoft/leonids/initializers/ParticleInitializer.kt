package com.plattysoft.leonids.initializers

import com.plattysoft.leonids.Particle
import java.util.*

interface ParticleInitializer {

    fun initParticle(p: Particle, r: Random)

}
